package me.wlan222.plugins.aptitude;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Aptitude extends JavaPlugin {
	private String updateURL;

	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}

	public void onEnable() {
		if (!getConfig().isBoolean("console-only")) {
			getConfig().set("console-only", false);
		}
	}

	public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("apt")) {
			if (getConfig().getBoolean("console-only")) {
				if (!(sender instanceof Player)) {
					return false;
				}
			}
			if (sender instanceof Player) {
				Player p = (Player) sender;
				if (!p.hasPermission("aptitude.install")) {
					return false;
				}
			}
			if (args.length == 0) {
				sender.sendMessage("/apt <bukkitdev slug>");
				return true;
			}
			try {
				if (found(args[0])) {
					Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

						public void run() {
							try {
								download(sender);
							} catch (Exception e) {
								//e.printStackTrace();
								sender.sendMessage("The File couldn't be downloaded");
							}
						}

					});

				} else {
					sender.sendMessage("The Plugin was not found!");
					return true;
				}
			} catch (Exception e) {
				//e.printStackTrace();
				sender.sendMessage("A problem occured try again!");
			}

			return true;
		}
		return false;
	}

	private void download(CommandSender sender) throws Exception {
		if (updateURL == null) {
			return;
		}
		String old_name = updateURL.substring(updateURL.lastIndexOf('/') + 1, updateURL.length());
		File to = new File("plugins", old_name);

		while (to.exists()) {
			old_name = getNewName(old_name);
			to = new File("plugins", old_name);
		}
		URL url = new URL(updateURL);
		InputStream is = url.openStream();
		OutputStream os = new FileOutputStream(to);
		byte[] buffer = new byte[4096];
		int fetched;
		while ((fetched = is.read(buffer)) != -1)
			os.write(buffer, 0, fetched);
		is.close();
		os.flush();
		os.close();
		sender.sendMessage("Done");
		updateURL = null;
	}

	private boolean found(String string) throws Exception {
		InputStreamReader ir;
		URL url = new URL("http://api.bukget.org/api2/bukkit/plugin/" + string + "/latest");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.connect();
		int res = con.getResponseCode();
		if (res != 200) {
			return false;
		}
		ir = new InputStreamReader(con.getInputStream());
		try {
			JSONParser jp = new JSONParser();
			Object o = jp.parse(ir);

			if (!(o instanceof JSONObject)) {
				ir.close();
				return false;
			}

			JSONObject jo = (JSONObject) o;
			jo = (JSONObject) jo.get("versions");
			updateURL = (String) jo.get("download");
			ir.close();
		} catch (ParseException e) {

			ir.close();
			return false;
		}
		return true;
	}

	public String getNewName(String s) {
		return s + "_copy";
	}

}
