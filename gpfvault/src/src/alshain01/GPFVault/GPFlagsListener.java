/* Copyright 2013 Kevin Seiden. All rights reserved.

 This works is licensed under the Creative Commons Attribution-NonCommercial 3.0

 You are Free to:
    to Share — to copy, distribute and transmit the work
    to Remix — to adapt the work

 Under the following conditions:
    Attribution — You must attribute the work in the manner specified by the author (but not in any way that suggests that they endorse you or your use of the work).
    Non-commercial — You may not use this work for commercial purposes.

 With the understanding that:
    Waiver — Any of the above conditions can be waived if you get permission from the copyright holder.
    Public Domain — Where the work or any of its elements is in the public domain under applicable law, that status is in no way affected by the license.
    Other Rights — In no way are any of the following rights affected by the license:
        Your fair dealing or fair use rights, or other applicable copyright exceptions and limitations;
        The author's moral rights;
        Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights.

 Notice — For any reuse or distribution, you must make clear to others the license terms of this work. The best way to do this is with a link to this web page.
 http://creativecommons.org/licenses/by-nc/3.0/
*/

package src.alshain01.GPFVault;

import me.ryanhamshire.GriefPrevention.Claim;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import src.john01dav.GriefPreventionFlags.ClaimManager;
import src.john01dav.GriefPreventionFlags.Flag;
import src.john01dav.GriefPreventionFlags.events.FlagDeleteEvent;
import src.john01dav.GriefPreventionFlags.events.FlagSetEvent;
import src.john01dav.GriefPreventionFlags.events.MessageChangedEvent;

/**
 * Event Handler for GriefPreventionFlags
 * 
 * @author Kevin Seiden
 */
public class GPFlagsListener implements Listener {
	/**
	 * 
	 * @param transaction The type of the transaction
	 * @param type The type of charge (Flag or Message)
	 * @param flag The flag
	 * @param player The player
	 * @return True if the event should be cancelled due to insufficient funds or error.
	 */
	private static boolean makeTransaction(ETransactionType transaction, EPurchaseType product, Flag flag, Player player) {
		// Get the YAML data path of the price.
		String pricePath = "Price." + product.toString() + "." + flag.getType().toString();

		// Get the price as a string
		String setting = GPFVault.instance.getConfig().getString(pricePath);
		
		// Plugin has not been configured for this flag, so it's assumed free.		
		if(setting == null) { return false; }

		// Convert the price to a number
		Double cost;
		try {
			cost = Double.valueOf(setting);
		} catch (NumberFormatException ex) {
			// The price isn't configured correctly.  We don't know what to charge so we prevent the transaction.
			GPFVault.instance.getLogger()
					.severe("<Price Error> " + product.toString() + ":" + flag.getType().toString() + " Cost: " + setting);
			
			player.sendMessage(EMessages.SetError.get()
					.replaceAll("<0>", product.getLocal().toLowerCase()));
			return true;
		}
		
		// Plugin was configured for flag to be free.
		if (cost == 0) { return false; } 

		
		EconomyResponse r;
		if (transaction == ETransactionType.Withdraw) {
			// Check to see if they have the money.
			if (cost > GPFVault.instance.economy.getBalance(player.getName())) {
				player.sendMessage(EMessages.LowFunds.get()
						.replaceAll("<0>", product.getLocal().toLowerCase())
						.replaceAll("<1>", GPFVault.instance.economy.format(cost))
						.replaceAll("<2>", flag.getType().getLocalName()));
				return true;
			}
		
			// They have the money, make transaction
			r = GPFVault.instance.economy.withdrawPlayer(player.getName(), cost);
		} else {
			// Deposit
			r = GPFVault.instance.economy.depositPlayer(player.getName(), cost);
		}
		
		if (r.transactionSuccess()) {
			player.sendMessage(transaction.getLocal()
					.replaceAll("<1>", GPFVault.instance.economy.format(cost)));
			return false;
		}
		
		// Something went wrong if we made it this far.
		GPFVault.instance.getLogger().severe(String.format("An error occured: %s", r.errorMessage));
		player.sendMessage(EMessages.Error.get()
				.replaceAll("<3>", r.errorMessage));
		return true;
						
	}
	
	/**
	 * Event handler for FlagSetEvent
	 * 
	 * @param e The event data set.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private static void onFlagSet(FlagSetEvent e) {
		// Don't charge for admin claims & if there is no player, we can't do anything
		if (e.getClaim().isAdminClaim()) { return; }
		if (e.getPlayer() == null) { return; }
		
		// Acquire the flag
		Flag flag = new Flag(e.getFlagType());

		// Check whether or not to charge the account
		if (EBaseFlagValue.ALWAYS.isSet() 
				|| (EBaseFlagValue.DEFAULT.isSet() && e.getNewValue() != flag.getType().getDefault()) 
				|| (EBaseFlagValue.GLOBAL.isSet() && e.getNewValue() != flag.getValue()))
		{ 
			// Charge the account
			e.setCancelled(makeTransaction(ETransactionType.Withdraw, EPurchaseType.Flag, flag, e.getPlayer()));
			return;
		}

		// Check whether or not to refund the account
		if (EPurchaseType.Flag.isRefundable() && !EBaseFlagValue.ALWAYS.isSet()) {
			makeTransaction(ETransactionType.Deposit, EPurchaseType.Flag, flag, e.getPlayer());
			return;
		}
	}
	
	/**
	 * Event handler for FlagDeleteEvent
	 * 
	 * @param e The event data set.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private static void onFlagDelete(FlagDeleteEvent e) {
		// Don't charge for admin claims & if there is no player, we can't do anything
		if (e.getClaim().isAdminClaim()) { return; }
		if (e.getPlayer() == null) { return; }
		
		// Check whether or not to refund the account
		if (!EPurchaseType.Flag.isRefundable()) { return; }
		
		// Acquire the flag & claim
		Flag flag = new Flag(e.getFlagType());
		Claim claim = ClaimManager.getClaimAtLocation(e.getPlayer().getLocation());
		
		// Check whether or not to refund the account
		if (EBaseFlagValue.ALWAYS.isSet() 
				|| (EBaseFlagValue.DEFAULT.isSet() && flag.getValue(claim) != flag.getType().getDefault()) 
				|| (EBaseFlagValue.GLOBAL.isSet() && flag.getValue(claim) != flag.getValue()))
	    { 
			makeTransaction(ETransactionType.Deposit, EPurchaseType.Flag, flag, e.getPlayer());
			return;
		}
	}
	
	/**
	 * Event handler for MessageChangedEvent
	 * 
	 * @param e The event data set.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private static void onMessageChanged(MessageChangedEvent e) {
		// Don't charge for admin claims & if there is no player, we can't do anything
		if (e.getClaim().isAdminClaim()) { return; }
		if (e.getPlayer() == null) { return; }
		
		// Acquire the flag & claim
		Flag flag = new Flag(e.getFlagType());
		Claim claim = ClaimManager.getClaimAtLocation(e.getPlayer().getLocation());
		
		// Check to make sure we aren't removing the message
		if (e.getMessage() != null) {
			// Check to make sure the message isn't identical to what we have
			if(flag.getMessage(claim).equals(e.getMessage().replaceAll("<1>", claim.getOwnerName()))) {	return; }
			
			// Charge the account
			e.setCancelled(makeTransaction(ETransactionType.Withdraw, EPurchaseType.Message, new Flag(e.getFlagType()), e.getPlayer()));
			return;
		}
		
		// If we got this far, the flag is being removed
		// Check whether or not to refund the account
		if (!EPurchaseType.Message.isRefundable()) { return; }
		
		// Make sure the message we are refunding isn't identical to the global message
		if (!(flag.getMessage(claim).equals(e.getFlagType().getMessage().replaceAll("<1>", claim.getOwnerName())))) {
			makeTransaction(ETransactionType.Deposit, EPurchaseType.Message,  new Flag(e.getFlagType()), e.getPlayer());
		}
	}
}