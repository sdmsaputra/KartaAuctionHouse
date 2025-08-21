package com.minekartastudio.kartaauctionhouse.mailbox.model;

import com.minekartastudio.kartaauctionhouse.common.SerializedItem;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record MailboxEntry(
    UUID id,
    UUID owner,
    MailboxType type,
    @Nullable SerializedItem item,
    @Nullable Double amount,
    String note,
    long createdAt,
    boolean claimed
) {
    public MailboxEntry withClaimed(boolean claimedStatus) {
        return new MailboxEntry(id, owner, type, item, amount, note, createdAt, claimedStatus);
    }
}
