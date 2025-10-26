package com.minekartastudio.kartaauctionhouse.storage.yaml;

import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.common.SerializedItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YamlUtil {
    public static Map<String, Object> auctionToMap(Auction auction) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", auction.id().toString());
        map.put("seller", auction.seller().toString());
        map.put("item", auction.item().getBase64());
        map.put("price", auction.price());
        map.put("createdAt", auction.createdAt());
        map.put("endAt", auction.endAt());
        map.put("status", auction.status().name());
        map.put("version", auction.version());
        if (auction.winner() != null) map.put("winner", auction.winner().toString());
        return map;
    }

    public static Auction auctionFromMap(Map<String, Object> map) {
        return new Auction(
                UUID.fromString((String) map.get("id")),
                UUID.fromString((String) map.get("seller")),
                SerializedItem.fromBase64((String) map.get("item")),
                (Double) map.get("price"),
                ((Number) map.get("createdAt")).longValue(),
                ((Number) map.get("endAt")).longValue(),
                AuctionStatus.valueOf((String) map.get("status")),
                (Integer) map.get("version"),
                map.get("winner") != null ? UUID.fromString((String) map.get("winner")) : null
        );
    }

    public static Map<String, Object> mailboxToMap(com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entry.id().toString());
        map.put("owner", entry.owner().toString());
        map.put("type", entry.type().name());
        if (entry.item() != null) map.put("item", entry.item().getBase64());
        if (entry.amount() != null) map.put("amount", entry.amount());
        map.put("note", entry.note());
        map.put("createdAt", entry.createdAt());
        map.put("claimed", entry.claimed());
        return map;
    }

    public static com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxEntry mailboxFromMap(Map<String, Object> map) {
        return new com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxEntry(
                UUID.fromString((String) map.get("id")),
                UUID.fromString((String) map.get("owner")),
                com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxType.valueOf((String) map.get("type")),
                map.containsKey("item") ? SerializedItem.fromBase64((String) map.get("item")) : null,
                (Double) map.get("amount"),
                (String) map.get("note"),
                ((Number) map.get("createdAt")).longValue(),
                (Boolean) map.get("claimed")
        );
    }

    public static Map<String, Object> transactionToMap(com.minekartastudio.kartaauctionhouse.transaction.model.Transaction transaction) {
        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", transaction.transactionId().toString());
        map.put("auctionId", transaction.auctionId().toString());
        map.put("sellerUuid", transaction.sellerUuid().toString());
        if (transaction.buyerUuid() != null) map.put("buyerUuid", transaction.buyerUuid().toString());
        map.put("itemSnapshot", transaction.itemSnapshot().getBase64());
        if (transaction.finalPrice() != null) map.put("finalPrice", transaction.finalPrice());
        map.put("status", transaction.status());
        map.put("timestamp", transaction.timestamp());
        return map;
    }

    public static com.minekartastudio.kartaauctionhouse.transaction.model.Transaction transactionFromMap(Map<String, Object> map) {
        return new com.minekartastudio.kartaauctionhouse.transaction.model.Transaction(
                UUID.fromString((String) map.get("transactionId")),
                UUID.fromString((String) map.get("auctionId")),
                UUID.fromString((String) map.get("sellerUuid")),
                map.containsKey("buyerUuid") ? UUID.fromString((String) map.get("buyerUuid")) : null,
                SerializedItem.fromBase64((String) map.get("itemSnapshot")),
                (Double) map.get("finalPrice"),
                (String) map.get("status"),
                ((Number) map.get("timestamp")).longValue()
        );
    }
}
