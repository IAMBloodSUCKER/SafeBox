package com.safebox.service;

import com.safebox.model.PasswordEntry;
import com.safebox.util.I18n;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

/**
 * Serializes vault entries to a portable binary format.
 */
final class VaultTransferCodec {

    private static final byte VERSION = 1;

    private VaultTransferCodec() {
    }

    static byte[] serialize(List<PasswordEntry> entries) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeByte(VERSION);
            out.writeInt(entries.size());
            for (PasswordEntry entry : entries) {
                writeString(out, entry.getSite());
                writeString(out, entry.getLogin());
                writeString(out, entry.getPassword());
                writeString(out, entry.getNotes());
                out.writeLong(toEpochMillis(entry.getCreatedAt()));
                out.writeLong(toEpochMillis(entry.getUpdatedAt()));
            }
        }
        return buffer.toByteArray();
    }

    static List<PasswordEntry> deserialize(byte[] payload) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            byte version = in.readByte();
            if (version != VERSION) {
                throw new VaultTransferService.TransferException(I18n.get("error.transfer.unsupportedVersion"));
            }
            int count = in.readInt();
            if (count < 0 || count > 100_000) {
                throw new VaultTransferService.TransferException(I18n.get("error.transfer.invalidFile"));
            }
            List<PasswordEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                PasswordEntry entry = new PasswordEntry();
                entry.setSite(readString(in));
                entry.setLogin(readString(in));
                entry.setPassword(readString(in));
                entry.setNotes(readString(in));
                entry.setCreatedAt(fromEpochMillis(in.readLong()));
                entry.setUpdatedAt(fromEpochMillis(in.readLong()));
                entries.add(entry);
            }
            if (in.available() > 0) {
                throw new VaultTransferService.TransferException(I18n.get("error.transfer.invalidFile"));
            }
            return entries;
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 65_535) {
            throw new VaultTransferService.TransferException(I18n.get("error.transfer.fieldTooLong"));
        }
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) {
            throw new VaultTransferService.TransferException(I18n.get("error.transfer.invalidFile"));
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static long toEpochMillis(Instant instant) {
        return instant == null ? 0L : instant.toEpochMilli();
    }

    private static Instant fromEpochMillis(long millis) {
        return millis <= 0L ? Instant.now() : Instant.ofEpochMilli(millis);
    }
}
