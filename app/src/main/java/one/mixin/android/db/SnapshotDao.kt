package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem

@Dao
interface SnapshotDao : BaseDao<Snapshot> {
    companion object {
        const val SNAPSHOT_ITEM_PREFIX =
            "SELECT s.*, u.full_name AS opponentFullName, a.symbol AS asset_symbol, a.confirmations AS asset_confirmations, u.avatar_url AS avatarUrl " +
            "FROM snapshots s " +
            "LEFT JOIN users u ON u.user_id = s.opponent_id " +
            "LEFT JOIN assets a ON a.asset_id = s.asset_id "
    }

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshots(assetId: String): LiveData<List<SnapshotItem>>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId ORDER BY s.amount * a.price_usd DESC, s.snapshot_id DESC")
    fun snapshotsOrderByAmount(assetId: String): LiveData<List<SnapshotItem>>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId AND (s.type = :type OR s.type =:otherType) ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshotsByType(assetId: String, type: String, otherType: String? = null): LiveData<List<SnapshotItem>>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId AND (s.type = :type OR s.type =:otherType) ORDER BY s.amount * a.price_usd DESC, s.snapshot_id DESC")
    fun snapshotsByTypeOrderByAmount(assetId: String, type: String, otherType: String? = null): LiveData<List<SnapshotItem>>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId and snapshot_id = :snapshotId")
    fun snapshotLocal(assetId: String, snapshotId: String): SnapshotItem?

    @Query("$SNAPSHOT_ITEM_PREFIX ORDER BY s.created_at DESC")
    fun allSnapshots(): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX ORDER BY s.amount * a.price_usd DESC")
    fun allSnapshotsOrderByAmount(): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE (s.type = :type OR s.type =:otherType) ORDER BY s.created_at DESC")
    fun allSnapshotsByType(type: String, otherType: String? = null): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE (s.type = :type OR s.type =:otherType) ORDER BY s.amount * a.price_usd DESC")
    fun allSnapshotsByTypeOrderByAmount(type: String, otherType: String? = null): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.type != 'pending' AND s.opponent_id = :opponentId ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshotsByUserId(opponentId: String): LiveData<List<SnapshotItem>>

    @Query("DELETE FROM snapshots WHERE type = 'pending' AND asset_id = :assetId")
    fun clearPendingDepositsByAssetId(assetId: String)

    @Query("DELETE FROM snapshots WHERE type = 'pending' AND transaction_hash = :transactionHash")
    fun deletePendingSnapshotByHash(transactionHash: String)
}