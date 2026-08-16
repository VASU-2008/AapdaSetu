package com.example.aapdasetu

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object AudioUtils {

    /*
     * Version marker for compressed voice payload.
     *
     * V1:
     * GZIP compressed audio bytes
     * then Base64 encoded.
     */
    private const val VOICE_PREFIX = "ASVOICE1:"

    /**
     * Convert an audio file into a compressed Base64 string.
     *
     * The original audio bytes are compressed with GZIP first.
     */
    fun fileToBase64(file: File): String {

        require(file.exists()) {
            "Audio file does not exist"
        }

        require(file.length() > 0) {
            "Audio file is empty"
        }

        val originalBytes =
            file.readBytes()

        val compressed =
            ByteArrayOutputStream().use { output ->

                GZIPOutputStream(output).use { gzip ->

                    gzip.write(
                        originalBytes
                    )
                }

                output.toByteArray()
            }

        val encoded =
            Base64.encodeToString(
                compressed,
                Base64.NO_WRAP
            )

        return VOICE_PREFIX + encoded
    }

    /**
     * Decode a voice payload received from AapdaSetu.
     *
     * Supports:
     *
     * ASVOICE1:<gzip + base64>
     *
     * and also old uncompressed Base64 payloads.
     */
    fun base64ToPlaybackFile(
        cacheDir: File,
        base64Audio: String,
        messageId: String
    ): File? {

        return try {

            if (base64Audio.isBlank()) {
                return null
            }

            val bytes =
                if (
                    base64Audio.startsWith(
                        VOICE_PREFIX
                    )
                ) {

                    val encoded =
                        base64Audio.removePrefix(
                            VOICE_PREFIX
                        )

                    val compressed =
                        Base64.decode(
                            encoded,
                            Base64.NO_WRAP
                        )

                    decompress(
                        compressed
                    )

                } else {

                    /*
                     * Backwards compatibility with
                     * old AapdaSetu voice messages.
                     */
                    Base64.decode(
                        base64Audio,
                        Base64.NO_WRAP
                    )
                }

            if (bytes.isEmpty()) {
                return null
            }

            val file =
                File(
                    cacheDir,
                    "voice_${messageId}.amr"
                )

            FileOutputStream(file).use {
                it.write(bytes)
                it.flush()
            }

            if (
                !file.exists() ||
                file.length() == 0L
            ) {
                return null
            }

            file

        } catch (
            _: Exception
        ) {

            null
        }
    }

    /**
     * GZIP decompression.
     */
    private fun decompress(
        compressed: ByteArray
    ): ByteArray {

        return GZIPInputStream(
            ByteArrayInputStream(
                compressed
            )
        ).use { gzip ->

            val output =
                ByteArrayOutputStream()

            val buffer =
                ByteArray(8192)

            while (true) {

                val count =
                    gzip.read(buffer)

                if (count <= 0) {
                    break
                }

                output.write(
                    buffer,
                    0,
                    count
                )
            }

            output.toByteArray()
        }
    }
}