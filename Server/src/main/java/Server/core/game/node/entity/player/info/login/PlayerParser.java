package core.game.node.entity.player.info.login;

import core.ServerConstants;
import core.game.node.entity.combat.CombatSpell;
import core.game.node.entity.player.Player;
import core.game.node.entity.player.link.SpellBookManager;
import core.game.node.entity.player.link.emote.Emotes;
import core.game.system.SystemLogger;
import core.game.world.map.Location;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;

/**
 * Handles the parsing and dumping of a player's character file.
 * @author Emperor
 */
public final class PlayerParser {

	/**
	 * Parses a player's character file.
	 * @param player The player.
	 */
	@SuppressWarnings("deprecation")
	public static void parse(Player player) {
		player.getGameAttributes().parse(player.getName() + ".xml");
		try {
			new PlayerSaveParser(player).parse();
		} catch (Exception e) {
			e.printStackTrace();
			final File file = new File((ServerConstants.PLAYER_SAVE_PATH + player.getName() + ".save"));
			if (!file.exists()) {
				makeFromTemplate(player);
				parse(player);
				return;
			}
			try (RandomAccessFile raf = new RandomAccessFile(file, "r"); FileChannel channel = raf.getChannel()) {
				ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
				int opcode;
				long networth = 0;
				int[] opcodeLog = new int[5];
				while ((opcode = buffer.get() & 0xFF) != 0) {
					switch (opcode) {
						case 1:
							player.setLocation(Location.create(buffer.getShort() & 0xFFFF, buffer.getShort() & 0xFFFF, buffer.get() & 0xF));
							break;
						case 2:
							networth += player.getInventory().parse(buffer);
							break;
						case 3:
							networth += player.getEquipment().parse(buffer);
							break;
						case 4:
							networth += player.getBank().parse(buffer);
							break;
						case 5:
							player.getSkills().parse(buffer);

							break;
						case 6:
							player.getSettings().parse(buffer);
							break;
						case 7://old emotes
							int op;
							while ((op = buffer.get() & 0xFF) != 0) {
								switch (op) {
									default: // Opcodes 22-40 are used for locked emotes.
										player.getEmoteManager().unlock(Emotes.values()[op]);
										break;
								}
							}
							break;
						case 10:
							break;
						case 14:
							player.getSlayer().parse(buffer);
							break;
						case 17:
							player.getQuestRepository().parse(buffer);
							break;
						case 21:
							player.getAppearance().parse(buffer);
							break;
						case 23:
							player.getGraveManager().parse(buffer);
							break;
						case 25:
							player.getSpellBookManager().parse(buffer);
							break;
						case 26:
							player.getGrandExchange().parse(buffer);
							break;
						case 27:
							player.getSavedData().parse(buffer);
							break;
						case 28:
							player.getDetails().getCommunication().parsePrevious(buffer);
							break;
						case 29:
							int spellBook = buffer.get();
							int spellId = buffer.get() & 0xFF;
							player.getProperties().setAutocastSpell((CombatSpell) SpellBookManager.SpellBook.values()[spellBook].getSpell(spellId));
							break;
						case 30:
							player.getFarmingManager().parse(buffer);
							break;
						case 31:
							player.getConfigManager().parse(buffer);
							break;
						case 32:
							player.getWarningMessages().parse(buffer);
							break;
						case 33:
							player.getMonitor().parse(buffer);
							break;
						case 34:
							player.getMusicPlayer().parse(buffer);
							break;
						case 35:
							player.getFamiliarManager().parse(buffer);
							break;
						case 36:
							player.getBarcrawlManager().parse(buffer);
							break;
						case 37:
							player.getStateManager().parse(buffer);
							break;
						case 38:
							player.getAntiMacroHandler().parse(buffer);
							break;
						case 39:
							player.getTreasureTrailManager().parse(buffer);
							break;
						case 40:
							player.getBankPinManager().parse(buffer);
							break;
						case 41:
							player.getHouseManager().parse(buffer);
							break;
						case 42:
							player.getAchievementDiaryManager().parse(buffer);
							break;
						case 43:
							player.getIronmanManager().parse(buffer);
							break;
						case 44:
							player.getEmoteManager().parse(buffer);
							break;
						case 45:
							player.getSkills().setCombatMilestone(buffer.get() & 0xFF);
							player.getSkills().setSkillMilestone(buffer.get() & 0xFF);
							break;
						case 46:
							player.getSkills().parseExpRate(buffer);
							break;
						case 47:
							player.getStatisticsManager().parse(buffer);
							break;
						case 48:
							player.getBrawlingGlovesManager().parse(buffer);
							break;
						default:
							System.err.println("[Player parsing] Unhandled opcode: " + opcode + " for " + player.getName() + " - [log=" + Arrays.toString(opcodeLog) + "].");
							break;
					}
					for (int i = opcodeLog.length - 2; i >= 0; i--) {
						opcodeLog[i + 1] = opcodeLog[i];
					}
					opcodeLog[0] = opcode;
				}
				player.getMonitor().setNetworth(networth);
				raf.close();
				channel.close();
			} catch (IOException f) {
				f.printStackTrace();
				player.setAttribute("protect_data", true);
			}
		}
	}

	/**
	 * Dumps the player's details to the character file.
	 * @param player The player.
	 * @param directory The saving directory.
	 */
	public static void dump(Player player) {
		new PlayerSaver(player).save();
	}

	public static void makeFromTemplate(Player player){
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream("data/players/template/template.json");
			os = new FileOutputStream("data/players/" + player.getName() + ".json");
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} catch (Exception e){
		} finally {
			try {
				is.close();
				os.close();
			} catch (Exception f){
				SystemLogger.log("Unable to close file copiers.");
			}
		}
	}
}