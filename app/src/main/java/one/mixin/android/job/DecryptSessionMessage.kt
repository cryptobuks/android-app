package one.mixin.android.job

import android.app.Activity
import android.app.NotificationManager
import android.util.Log
import one.mixin.android.MixinApplication
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.extension.findLastUrl
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.createReplyMessage
import one.mixin.android.vo.createStickerMessage
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.ACKNOWLEDGE_SESSION_MESSAGE_RECEIPTS
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.SystemConversationData
import one.mixin.android.websocket.SystemExtensionSessionAction
import one.mixin.android.websocket.TransferPlainAckData
import one.mixin.android.websocket.TransferStickerData
import org.whispersystems.libsignal.DecryptionCallback
import java.util.UUID

class DecryptSessionMessage : Injector() {

    companion object {
        val TAG = DecryptSessionMessage::class.java.simpleName
    }

    private val gson = GsonHelper.customGson
    private val notificationManager: NotificationManager by lazy {
        MixinApplication.appContext.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun onRun(data: BlazeMessageData) {
        syncConversation(data)
        processSignalMessage(data)
        processPlainMessage(data)
        processSystemMessage(data)
    }

    private fun processSignalMessage(data: BlazeMessageData) {
        if (!data.category.startsWith("SIGNAL_")) {
            return
        }
        val (keyType, cipherText, _) = SignalProtocol.decodeMessageData(data.data)
        val deviceId = UUID.fromString(data.sessionId).hashCode()
        try {
            signalProtocol.decrypt(data.conversationId, data.userId, keyType, cipherText, data.category, DecryptionCallback {
                if (!data.primitiveId.isNullOrBlank()) {
                    data.userId = data.primitiveId
                }
                processDecryptSuccess(data, String(it))
            }, deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "process session signal message", e)
        }
        updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
    }

    private fun processSystemMessage(data: BlazeMessageData) {
        if (!data.category.startsWith("SYSTEM_")) {
            return
        }
        if (data.category == MessageCategory.SYSTEM_EXTENSION_SESSION.name) {
            val json = Base64.decode(data.data)
            val systemMessage = gson.fromJson(String(json), SystemConversationData::class.java)
            if (systemMessage.action == SystemExtensionSessionAction.ADD_SESSION.name && data.sessionId != null) {
                Session.storeExtensionSessionId(data.sessionId)
                signalProtocol.deleteSession(data.userId)
            } else if (systemMessage.action == SystemExtensionSessionAction.REMOVE_SESSION.name && data.sessionId != null) {
                Session.deleteExtensionSessionId(data.sessionId)
                signalProtocol.deleteSession(data.userId)
            }
        }
        updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
    }

    private fun processPlainMessage(data: BlazeMessageData) {
        if (!data.category.startsWith("PLAIN_")) {
            return
        }
        if (data.category == MessageCategory.PLAIN_TEXT.name ||
            data.category == MessageCategory.PLAIN_IMAGE.name ||
            data.category == MessageCategory.PLAIN_VIDEO.name ||
            data.category == MessageCategory.PLAIN_DATA.name ||
            data.category == MessageCategory.PLAIN_AUDIO.name ||
            data.category == MessageCategory.PLAIN_STICKER.name ||
            data.category == MessageCategory.PLAIN_CONTACT.name) {
            if (!data.representativeId.isNullOrBlank()) {
                data.userId = data.representativeId
            }
            processDecryptSuccess(data, data.data)
            updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
        } else if (data.category == MessageCategory.PLAIN_JSON.name) {
            val json = Base64.decode(data.data)
            val plainData = gson.fromJson(String(json), TransferPlainAckData::class.java)
            if (plainData.action == PlainDataAction.ACKNOWLEDGE_MESSAGE_RECEIPTS.name) {
                for (m in plainData.messages) {
                    if (m.status != MessageStatus.READ.name) {
                        continue
                    }
                    val curStatus = messageDao.findMessageStatusById(m.message_id)
                    if (curStatus != null && MessageStatus.valueOf(m.status) > MessageStatus.valueOf(curStatus)) {
                        messageDao.updateMessageStatus(m.status, m.message_id)
                        messageDao.findConversationById(m.message_id)?.let { conversationId ->
                            messageDao.takeUnseen(Session.getAccountId()!!, conversationId)
                            notificationManager.cancel(conversationId.hashCode())
                        }
                        jobDao.insert(createAckJob(ACKNOWLEDGE_MESSAGE_RECEIPTS, m))
                    }
                }
                updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
            }
        }
    }

    private fun updateRemoteMessageStatus(messageId: String, status: MessageStatus = MessageStatus.DELIVERED) {
        jobDao.insert(createAckJob(ACKNOWLEDGE_SESSION_MESSAGE_RECEIPTS, BlazeAckMessage(messageId, status.name)))
    }

    private fun processDecryptSuccess(data: BlazeMessageData, plainText: String) {
        when {
            data.category.endsWith("_TEXT") -> {
                val plain = if (data.category == MessageCategory.PLAIN_TEXT.name) String(Base64.decode(plainText)) else plainText
                val message = if (data.quoteMessageId == null) {
                    createMessage(data.messageId, data.conversationId, data.userId, data.category,
                        plain, data.createdAt, MessageStatus.SENDING)
                        .apply {
                            this.content?.findLastUrl()?.let { jobManager.addJobInBackground(ParseHyperlinkJob(it, data.messageId)) }
                        }
                } else {
                    val quoteMsg = messageDao.findMessageItemById(data.conversationId, data.quoteMessageId)
                    if (quoteMsg != null) {
                        createReplyMessage(data.messageId, data.conversationId, data.userId, data.category,
                            plain, data.createdAt, MessageStatus.SENDING, data.quoteMessageId, gson.toJson(quoteMsg))
                    } else {
                        createReplyMessage(data.messageId, data.conversationId, data.userId, data.category,
                            plain, data.createdAt, MessageStatus.SENDING, data.quoteMessageId)
                    }
                }

                messageDao.insert(message)
                jobManager.addJobInBackground(SendMessageJob(message, alreadyExistMessage = true))
            }
            data.category.endsWith("_STICKER") -> {
                val decoded = Base64.decode(plainText)
                val mediaData = gson.fromJson(String(decoded), TransferStickerData::class.java)
                val message = if (mediaData.stickerId == null) {
                    val sticker = stickerDao.getStickerByAlbumIdAndName(mediaData.albumId!!, mediaData.name!!)
                    if (sticker != null) {
                        createStickerMessage(data.messageId, data.conversationId, data.userId, data.category, null,
                            mediaData.albumId, sticker.stickerId, mediaData.name, MessageStatus.SENDING, data.createdAt)
                    } else {
                        return
                    }
                } else {
                    val sticker = stickerDao.getStickerByUnique(mediaData.stickerId)
                    if (sticker == null) {
                        jobManager.addJobInBackground(RefreshStickerJob(mediaData.stickerId))
                    }
                    createStickerMessage(data.messageId, data.conversationId, data.userId, data.category, null,
                        mediaData.albumId, mediaData.stickerId, mediaData.name, MessageStatus.SENDING, data.createdAt)
                }
                messageDao.insert(message)
                jobManager.addJobInBackground(SendMessageJob(message, alreadyExistMessage = true))
            }
        }
    }
}