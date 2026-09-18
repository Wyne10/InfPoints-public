package org.bigcraft.infpoints.vault;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bigcraft.infpoints.Messages;
import org.bigcraft.infpoints.api.Point;
import org.bigcraft.infpoints.api.transaction.TransactionRequest;
import org.bigcraft.infpoints.api.transaction.TransactionResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@SuppressWarnings("deprecation")
public final class VaultEconomy implements Economy {

    private static final String NO_BANKS = "InfPoints doesn't support banks";

    private final Supplier<@Nullable Point> point;

    VaultEconomy(Supplier<@Nullable Point> point) {
        this.point = point;
    }

    @Override
    public boolean isEnabled() {
        Point current = point.get();
        return current != null && current.isAvailable();
    }

    @Override
    public String getName() {
        return "InfPoints";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        Point current = point.get();
        return current == null ? -1 : current.getConfig().decimals();
    }

    @Override
    public String format(double amount) {
        Point current = point.get();
        return current == null ? String.valueOf(amount) : Messages.format(current, amount);
    }

    @Override
    public String currencyNamePlural() {
        Point current = point.get();
        return current == null ? "" : current.getVisualConfig().pluralName();
    }

    @Override
    public String currencyNameSingular() {
        Point current = point.get();
        return current == null ? "" : current.getVisualConfig().name();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        Point current = point.get();
        return current != null && (current.supportsOfflinePlayers() || player.isOnline());
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        Point current = point.get();
        return current == null ? 0 : current.get(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return change(player, amount, TransactionRequest::subtract, "Cannot withdraw negative funds");
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return change(player, amount, TransactionRequest::add, "Cannot deposit negative funds");
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    private EconomyResponse change(OfflinePlayer player, double amount, BiFunction<UUID, Double, TransactionRequest> request, String negativeMessage) {
        Point current = point.get();
        if (current == null)
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Economy is unavailable");
        if (!Double.isFinite(amount))
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Invalid amount");
        if (amount < 0)
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, negativeMessage);
        if (amount == 0)
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
        TransactionResult result = current.execute(request.apply(player.getUniqueId(), amount));
        double balance = Double.isNaN(result.balance()) || result.status() == TransactionResult.Status.DUPLICATE
                ? current.get(player.getUniqueId())
                : result.balance();
        if (result.isApplied())
            return new EconomyResponse(result.amount(), balance, EconomyResponse.ResponseType.SUCCESS, null);
        return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, switch (result.status()) {
            case INSUFFICIENT_FUNDS -> "Insufficient funds";
            case INVALID_AMOUNT -> "Amount is smaller than the currency allows";
            case PLAYER_OFFLINE -> "Player must be online";
            case CANCELLED -> "Transaction was cancelled";
            case UNAVAILABLE -> "Economy is unavailable";
            default -> "Transaction failed";
        });
    }

}
