/*
 * Copyright (C) 2013-2016 XHawk87
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.xhawk87.Coinage.commands;

import me.xhawk87.Coinage.Coinage;
import me.xhawk87.Coinage.Currency;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author XHawk87
 */
public class SpendCoinsCommand extends CoinCommand {

    private Coinage plugin;

    public SpendCoinsCommand(Coinage plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getHelpMessage(CommandSender sender) {
        return "/SpendCoins [player] ([currency]) [value]. Remove the given value of coins from the specified player and give change where needed";
    }

    @Override
    public String getPermission() {
        return "coinage.commands.spendcoins";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) {
            return false;
        }

        int index = 0;
        String playerName = args[index++];
        Player player = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("There is no player matching " + playerName);
            return true;
        }

        Currency currency;
        if (args.length == 3) {
            String currencyName = args[index++];
            currency = plugin.getCurrency(currencyName);
            if (currency == null) {
                sender.sendMessage("There is no currency with id " + currencyName);
                return true;
            }
        } else {
            currency = plugin.getDefaultCurrency();
        }

        int value;
        String valueString = args[index++];
        try {

            value = Integer.parseInt(valueString);
            if (value < 1) {
                sender.sendMessage("The value must be at least 1 " + currency.toString() + ": " + valueString);
                return true;
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage("The value was not a valid number: " + valueString);
            return true;
        }

        if (currency.spend(player, value)) {
            if (sender != player) {
                sender.sendMessage(value + " in " + currency.toString() + " was deducted from " + player.getDisplayName());
            }
        } else {
            if (sender != player) {
                sender.sendMessage(player.getDisplayName() + " does not have " + value + " in " + currency.toString() + " to spend");
            }
        }
        return true;
    }
}
