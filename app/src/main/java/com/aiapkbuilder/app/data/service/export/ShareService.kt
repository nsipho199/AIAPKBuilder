package com.aiapkbuilder.app.data.service.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import com.aiapkbuilder.app.data.local.ShareLinkDao
import com.aiapkbuilder.app.data.model.ShareConfig
import com.aiapkbuilder.app.data.model.ShareLink
import com.aiapkbuilder.app.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareService @Inject constructor(
    private val shareLinkDao: ShareLinkDao,
    @ApplicationContext private val context: Context
) {
    private val logger = AppLogger.getLogger("ShareService")

    suspend fun generateShareLink(config: ShareConfig): Result<ShareLink> = withContext(Dispatchers.IO) {
        try {
            val token = UUID.randomUUID().toString().replace("-", "").take(16)
            val link = ShareLink(
                linkId = UUID.randomUUID().toString(),
                artifactId = config.artifactId,
                token = token,
                expiresAt = config.expirationDays?.let { System.currentTimeMillis() + (it * 24 * 60 * 60 * 1000L) },
                passwordHash = config.password?.let { hashPassword(it) },
                maxDownloads = config.maxDownloads,
                createdAt = System.currentTimeMillis()
            )
            shareLinkDao.insertLink(link)
            logger.i("Share link generated: ${link.linkId}")
            Result.success(link)
        } catch (e: Exception) {
            logger.e("Failed to generate share link", e)
            Result.failure(e)
        }
    }

    suspend fun getShareUrl(link: ShareLink): String {
        return "https://aiapkbuilder.io/share/${link.token}"
    }

    suspend fun getLinksForArtifact(artifactId: String): Flow<List<ShareLink>> {
        return shareLinkDao.getLinksForArtifact(artifactId)
    }

    suspend fun getActiveLinks(): Flow<List<ShareLink>> = shareLinkDao.getActiveLinks()

    suspend fun revokeLink(linkId: String) = withContext(Dispatchers.IO) {
        try {
            val link = shareLinkDao.getLink(linkId)
            if (link != null) {
                shareLinkDao.updateLink(link.copy(isActive = false))
            }
        } catch (e: Exception) {
            logger.e("Failed to revoke link", e)
        }
    }

    suspend fun recordDownload(linkId: String) = withContext(Dispatchers.IO) {
        try {
            val link = shareLinkDao.getLink(linkId)
            if (link != null) {
                shareLinkDao.updateLink(link.copy(downloadCount = link.downloadCount + 1))
            }
        } catch (e: Exception) {
            logger.e("Failed to record download", e)
        }
    }

    suspend fun recordView(linkId: String) = withContext(Dispatchers.IO) {
        try {
            val link = shareLinkDao.getLink(linkId)
            if (link != null) {
                shareLinkDao.updateLink(link.copy(viewCount = link.viewCount + 1))
            }
        } catch (e: Exception) {
            logger.e("Failed to record view", e)
        }
    }

    suspend fun shareViaIntent(file: File, mimeType: String, message: String) = withContext(Dispatchers.Main) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            logger.e("Failed to share via intent", e)
        }
    }

    suspend fun generateQRCode(content: String, size: Int = 512): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val matrix = generateQRMatrix(content, size)
            val scale = size.toFloat() / matrix.size

            for (y in matrix.indices) {
                for (x in matrix[y].indices) {
                    if (matrix[y][x]) {
                        canvas.drawRect(
                            x * scale, y * scale,
                            (x + 1) * scale, (y + 1) * scale,
                            android.graphics.Paint().apply { color = Color.BLACK }
                        )
                    }
                }
            }
            Result.success(bitmap)
        } catch (e: Exception) {
            logger.e("Failed to generate QR code", e)
            Result.failure(e)
        }
    }

    suspend fun saveQRCodeToFile(bitmap: Bitmap, fileName: String = "qrcode.png"): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "qrcodes")
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateQRMatrix(content: String, size: Int): Array<BooleanArray> {
        val data = content.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)

        val matrixSize = kotlin.math.sqrt(size.toDouble()).toInt().coerceAtLeast(21)
        val matrix = Array(matrixSize) { BooleanArray(matrixSize) }

        for (i in matrix.indices) {
            for (j in matrix[i].indices) {
                val idx = (i * matrixSize + j) % hash.size
                matrix[i][j] = (hash[idx].toInt() and 0x01) == 1
            }
        }
        return matrix
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    suspend fun cleanupExpiredLinks() = withContext(Dispatchers.IO) {
        shareLinkDao.deleteExpiredLinks()
    }
}
