package com.evilmidget38.parkourcraft;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class ParkourCraft extends JavaPlugin {
    public String defaultMessageColor = ChatColor.GREEN.toString();
    public HashMap<org.bukkit.block.Sign, ParkourMap> signs;
    public ArrayList<String> bannedcommands;
    public HashMap<Integer, ArrayList<String>> defaultRewards;
    public HashMap<String, ParkourMap> maps;
    boolean havVault;
    int defaultSize;
    String vipcheckpointmessage;
    int defaulthighscoresize;
    String mvpplayermessage;
    int deathBehavior;
    private Economy economy;

    public SignType getSignType(org.bukkit.block.Sign sign)
    {
        String[] text = sign.getLines();
        for (int i = 0; i < text.length; i++) {
            String s = text[i];
            if ((s != null) && (!s.equals("")))
                text[i] = s.toLowerCase();
        }
        if (text.length == 0) return SignType.NOTPARKOUR;
        if (!text[0].contains("[parkourcraft]")) return SignType.NOTPARKOUR;
        if (text.length < 3) return SignType.INVALID;
        if ((text[1].isEmpty()) && (text[2].contains("spawn"))) return SignType.INVALID;
        if ((text[2].isEmpty()) && (text[1].contains("spawn"))) return SignType.INVALID;
        if ((text[1].isEmpty()) && (text[2].contains("start"))) return SignType.INVALID;
        if ((text[2].isEmpty()) && (text[1].contains("start"))) return SignType.INVALID;
        if ((text[1].isEmpty()) && (text[2].contains("end"))) return SignType.INVALID;
        if ((text[2].isEmpty()) && (text[1].contains("end"))) return SignType.INVALID;
        if (text[2].contains("spawn")) return SignType.SPAWNLOCATION;
        if (text[2].contains("start")) return SignType.START;
        if (text[2].contains("end")) return SignType.END;
        if (text[1].contains("vipcheckpoint")) return SignType.VIPCHECKPOINT;
        if (text[1].contains("checkpoint")) return SignType.CHECKPOINT;
        if (text[2].contains("vipcheckpoint")) return SignType.VIPCHECKPOINT;
        if (text[2].contains("checkpoint")) return SignType.CHECKPOINT;
        return SignType.INVALID;
    }
    public String changeToColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    public void readConfig() {
        reloadConfig();
        if (!getConfig().contains("defaultchatcolor")) getConfig().set("defaultchatcolor", "green");
        if (!getConfig().contains("mvpplayermessage")) getConfig().set("mvpplayermessage", "This parkour map is currently full, but MVP donators can still get in");
        if (!getConfig().contains("vipcheckpointmessage")) getConfig().set("vipcheckpointmessage", "This checkpoint is for donators only");
        if (!getConfig().contains("retryondeath")) getConfig().set("retryondeath", Integer.valueOf(0));
        if (!getConfig().contains("defaultsize")) getConfig().set("defaultmapsize", Integer.valueOf(5));
        if (!getConfig().contains("defaulthighscoresize")) getConfig().set("defaulthighscoresize", Integer.valueOf(3));
        this.defaultMessageColor = changeToColors(getConfig().getString("defaultchatcolor"));
        this.mvpplayermessage = getConfig().getString("mvpplayermessage");
        this.defaulthighscoresize = getConfig().getInt("defaulthighscoresize");
        this.vipcheckpointmessage = getConfig().getString("vipcheckpointmessage");
        this.deathBehavior = getConfig().getInt("retryondeath");
        this.defaultSize = getConfig().getInt("defaultmapsize");
        saveConfig();
    }
    public void saveParkourMaps() {
        File f = new File(getDataFolder(), "ParkourMaps.yml");
        if (!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        else {
            try {
                f.delete();
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileConfiguration signConfig = YamlConfiguration.loadConfiguration(f);
        int i = 0;
        for (ParkourMap mp : this.maps.values())
            if (mp != null) {
                Location cp = mp.getLocation();
                ArrayList<String> plal = new ArrayList<String>();
                plal.add("X" + cp.getBlockX());
                plal.add("Y" + cp.getBlockY());
                plal.add("Z" + cp.getBlockZ());
                plal.add("W" + cp.getWorld().getName());
                signConfig.set(Integer.toString(i), plal);
                i++;
            }
        try {
            signConfig.save(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readParkourMaps() {
        File f = new File(getDataFolder(), "ParkourMaps.yml");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        FileConfiguration signConfig = YamlConfiguration.loadConfiguration(f);
        for (String s : signConfig.getKeys(false)) {
            if (!(signConfig.get(s) instanceof List)) {
                continue;
            }
            // TODO TBH I'm not exactly sure what's going on here, we'll look at it later.
            List<?> strl = signConfig.getList(s);
            boolean cont = false;
            String x = null; String y = null; String z = null; String w = null;
            for (int i = 0; i < strl.size(); i++) {
                if (!(strl.get(i) instanceof String)) {
                    cont = true;
                    break;
                }
                String tmp = (String)strl.get(i);
                if (tmp.indexOf("X") == 0) x = tmp;
                if (tmp.indexOf("Y") == 0) y = tmp;
                if (tmp.indexOf("Z") == 0) z = tmp;
                if (tmp.indexOf("W") != 0) continue; w = tmp;
            }

            if (!cont) {
                ParkLoc ploc = new ParkLoc(x, y, z, w);
                Location loc = getServer().getWorld(ploc.getWorld()).getBlockAt(ploc.getX(), ploc.getY(), ploc.getZ()).getLocation();
                if (!(loc.getBlock().getState() instanceof org.bukkit.block.Sign))
                {
                    continue;
                }
                org.bukkit.block.Sign sign = (org.bukkit.block.Sign)loc.getBlock().getState();
                addSignFromConfig(sign);
            }
        }
    }

    public void saveHighScores(ParkourMap pkm) {
        String filename = pkm.getName() + "scores.yml";
        File f = new File(getDataFolder(), filename);
        if (!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        else {
            try {
                f.delete();
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileConfiguration signConfig = YamlConfiguration.loadConfiguration(f);
        // Aren't foreach loops beautiful?
        for (Iterator<Long> localIterator = pkm.getHighscores().keySet().iterator(); localIterator.hasNext(); ) { 
            long l = ((Long)localIterator.next()).longValue();
            signConfig.set(Long.toString(l), pkm.getHighscores().get(Long.valueOf(l)));
        }
        try {
            signConfig.save(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readHighScores(ParkourMap pkm) {
        String filename = pkm.getName() + "scores.yml";
        File f = new File(getDataFolder(), filename);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        FileConfiguration signConfig = YamlConfiguration.loadConfiguration(f);
        HashMap<Long, String> hscore = new HashMap<Long, String>();
        for (String s : signConfig.getKeys(false)) {
            hscore.put(Long.parseLong(s), signConfig.getString(s));
        }
        pkm.setHighScores(hscore);
    }
    public void saveRewards(ParkourMap pkm) {
        String filename = pkm.getName() + ".yml";
        File f = new File(getDataFolder(), filename);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        FileConfiguration signConfig = YamlConfiguration.loadConfiguration(f);
        HashMap<Integer, ArrayList<String>> rewards = pkm.getRewards();
        signConfig.set("mapsize", Integer.valueOf(pkm.size));
        signConfig.set("highscoresize", Integer.valueOf(pkm.numhighscores));
        // Another one of them foreach loops.
        for (Iterator<Integer> localIterator = rewards.keySet().iterator(); localIterator.hasNext(); ) { int i = ((Integer)localIterator.next()).intValue();
        signConfig.set(Integer.toString(i), rewards.get(Integer.valueOf(i))); }
        try
        {
            signConfig.save(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readRewards(ParkourMap pkm) {
        String filename = pkm.getName() + ".yml";
        File f = new File(getDataFolder(), filename);
        if (!f.exists()) {
            getLogger().info("No custom file found for " + pkm.getName() + ". Default settings will be used");
            pkm.size = this.defaultSize;
            pkm.numhighscores = this.defaulthighscoresize;
            pkm.setRewards(this.defaultRewards);
            saveRewards(pkm);
            return;
        }
        FileConfiguration signConfig = YamlConfiguration.loadConfiguration(f);
        HashMap<Integer, ArrayList<String>> rew = new HashMap<Integer, ArrayList<String>>();
        for (String s : signConfig.getKeys(false)) {
            if (s.equals("mapsize"))
                pkm.size = signConfig.getInt(s);
            else if (s.equals("highscoresize"))
                pkm.numhighscores = signConfig.getInt(s);
            else {
                try
                {
                    List<String> lst = signConfig.getStringList(s);
                    ArrayList<String> al = new ArrayList<String>(lst);
                    rew.put(Integer.valueOf(Integer.parseInt(s)), al);
                } catch (Exception e) {
                    getLogger().severe("You have errors in your " + filename + " configuration file");
                }
            }
        }
        pkm.setRewards(rew);
        if (pkm.size == 0) {
            pkm.size = this.defaultSize;
            if (pkm.numhighscores == 0) {
                pkm.numhighscores = this.defaulthighscoresize;
            }
            if (rew.isEmpty()) {
                pkm.setRewards(this.defaultRewards);
            }
            saveRewards(pkm);
            return;
        }
        if (pkm.numhighscores == 0) {
            pkm.numhighscores = this.defaulthighscoresize;
            if (rew.isEmpty()) {
                pkm.setRewards(this.defaultRewards);
            }
            saveRewards(pkm);
            return;
        }
        if (rew.isEmpty()) {
            pkm.setRewards(this.defaultRewards);
            saveRewards(pkm);
            return;
        }
    }

    public Location setYaw(Location l, org.bukkit.material.Sign sign) {
        if (sign.getFacing().equals(BlockFace.NORTH)) l.setYaw(90.0F);
        if (sign.getFacing().equals(BlockFace.NORTH_EAST)) l.setYaw(135.0F);
        if (sign.getFacing().equals(BlockFace.EAST)) l.setYaw(180.0F);
        if (sign.getFacing().equals(BlockFace.SOUTH_EAST)) l.setYaw(225.0F);
        if (sign.getFacing().equals(BlockFace.SOUTH)) l.setYaw(270.0F);
        if (sign.getFacing().equals(BlockFace.SOUTH_WEST)) l.setYaw(315.0F);
        if (sign.getFacing().equals(BlockFace.WEST)) l.setYaw(0.0F);
        if (sign.getFacing().equals(BlockFace.NORTH_WEST)) l.setYaw(45.0F);
        return l;
    }

    public boolean addSignFromConfig(org.bukkit.block.Sign sign) {
        SignType type = getSignType(sign);

        if (type.equals(SignType.NOTPARKOUR))
        {
            return false;
        }
        if (type.equals(SignType.INVALID))
        {
            return false;
        }
        if (type.equals(SignType.SPAWNLOCATION)) {
            String name = sign.getLines()[1];
            if ((this.maps.containsKey(name)) && (this.maps.get(name) != null))
            {
                return false;
            }
            Location spawnloc = sign.getBlock().getLocation();
            org.bukkit.material.Sign sign2 = (org.bukkit.material.Sign)sign.getData();
            spawnloc = setYaw(spawnloc, sign2);
            ParkourMap pkm = new ParkourMap(this.defaultSize, name, spawnloc, this.defaulthighscoresize);
            readRewards(pkm);
            readHighScores(pkm);
            this.maps.put(name, pkm);
            if (this.havVault) pkm.setEconomy(this.economy);

            this.signs.put(sign, pkm);
            return true;
        }
        if (type.equals(SignType.CHECKPOINT))
        {
            return true;
        }
        if (type.equals(SignType.VIPCHECKPOINT))
        {
            return true;
        }
        if (type.equals(SignType.START)) {
            return true;
        }

        return type.equals(SignType.END);
    }

    public boolean addSignFromPlayer(org.bukkit.block.Sign sign, Player player)
    {
        SignType type = getSignType(sign);

        if (type.equals(SignType.NOTPARKOUR)) return true;
        if (!player.hasPermission("parkourcraft.create")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to create this ParkourCraft sign");
            return false;
        }
        if (type.equals(SignType.INVALID)) {
            player.sendMessage(ChatColor.RED + "Your ParkourCraft sign was invalid");
            return false;
        }
        if (type.equals(SignType.SPAWNLOCATION)) {
            String name = sign.getLine(1);
            if ((this.maps.containsKey(name)) && (this.maps.get(name) != null)) {
                player.sendMessage(ChatColor.RED + "There is already a parkour map named: " + name);
                return false;
            }
            Location spawnloc = sign.getBlock().getLocation();
            org.bukkit.material.Sign sign2 = (org.bukkit.material.Sign)sign.getData();
            spawnloc = setYaw(spawnloc, sign2);
            ParkourMap pkm = new ParkourMap(this.defaultSize, name, spawnloc, this.defaulthighscoresize);
            readRewards(pkm);
            readHighScores(pkm);
            this.maps.put(name, pkm);
            if (this.havVault) pkm.setEconomy(this.economy);

            this.signs.put(sign, pkm);
            return true;
        }
        if (type.equals(SignType.CHECKPOINT))
        {
            return true;
        }
        if (type.equals(SignType.VIPCHECKPOINT))
        {
            return true;
        }
        if (type.equals(SignType.START)) {
            String name = sign.getLines()[1];
            if (this.maps.get(name) == null) {
                player.sendMessage(ChatColor.RED + "Warning: " + name + " does not yet exist as a parkour map");
            }
            return true;
        }
        if (type.equals(SignType.END)) {
            String name = sign.getLines()[1];
            if (this.maps.get(name) == null) {
                player.sendMessage(ChatColor.RED + "Warning: " + name + " does not yet exist as a parkour map");
            }
            return true;
        }
        return true;
    }
    public boolean isAllowed(Player p, String cmd) {
        if (p.hasPermission("parkourcraft.cancommand")) return true;
        for (String s : this.bannedcommands) {
            if (s.equalsIgnoreCase(cmd)) return false;
        }
        return true;
    }

    public void removeDataFor(ParkourMap pkm)
    {
        String filename = pkm.getName() + "scores.yml";
        File f = new File(getDataFolder(), filename);
        f.delete();
        filename = pkm.getName() + ".yml";
        f = new File(getDataFolder(), filename);
        f.delete();
    }
    public void readCommands() {
        String filename = "bannedcommands.yml";
        File f = new File(getDataFolder(), filename);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        FileConfiguration signConfig = YamlConfiguration.loadConfiguration(f);
        for (String s : signConfig.getKeys(false)) {
            if (!signConfig.getBoolean(s)) continue; this.bannedcommands.add(s);
        }
    }

    public void readDefaultRewards() {
        String filename = "defaultrewards.yml";
        File f = new File(getDataFolder(), filename);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        FileConfiguration signConfig = YamlConfiguration.loadConfiguration(f);
        HashMap<Integer, ArrayList<String>> rew = new HashMap<Integer, ArrayList<String>>();
        for (String s : signConfig.getKeys(false))
        {
            List<String> lst = signConfig.getStringList(s);
            ArrayList<String> al = new ArrayList<String>(lst);
            rew.put(Integer.parseInt(s), al);
        }
        this.defaultRewards = rew;
    }
    public void onEnable() {
        this.havVault = false;
        this.bannedcommands = new ArrayList<String>();
        this.defaultRewards = new HashMap<Integer, ArrayList<String>>();
        this.maps = new HashMap<String, ParkourMap>();
        this.signs = new HashMap<org.bukkit.block.Sign, ParkourMap>();
        readCommands();
        readConfig();
        readDefaultRewards();
        readParkourMaps();
        setupVault();
        getServer().getPluginManager().registerEvents(new SignListener(), this);
        getLogger().info("ParkourCraft has been enabled");
    }
    private boolean setupEconomy() {
        try {
            if (getServer().getServicesManager().getRegistration(Economy.class) == null) return false; 
        }
        catch (NoClassDefFoundError e) {
            return false;
        }
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            this.economy = ((Economy)economyProvider.getProvider());
        }

        return this.economy != null;
    }
    public void giveMapsVault() {
        for (ParkourMap pkm : this.maps.values())
            if (pkm != null)
                pkm.setEconomy(this.economy);
    }

    public void setupVault() {
        if (!setupEconomy()) {
            getServer().getScheduler().runTaskLater(this, new Runnable()
            {
                public void run() {
                    boolean has = ParkourCraft.this.setupEconomy();
                    if (!has) {
                        ParkourCraft.this.getLogger().info("Vault not found. You cant still use ParkourCraft without it, but make sure rewards are not set to use Vault.");
                    } else {
                        ParkourCraft.this.havVault = true;
                        ParkourCraft.this.giveMapsVault();
                    }
                }
            }
            , 100L);
        } else {
            this.havVault = true;
            giveMapsVault();
        }
    }

    public void onDisable() {
        saveParkourMaps();
        for (ParkourMap pkm : this.maps.values()) {
            if (pkm == null) continue; pkm.removeAll();
        }
        getLogger().info("ParkourCraft has been disabled");
    }
    public void reload() {
        this.havVault = true;
        for (org.bukkit.block.Sign sign : this.signs.keySet()) {
            ParkourMap pks = (ParkourMap)this.signs.get(sign);

            this.maps.remove(pks.getName());
            this.maps.put(pks.getName(), null);

            pks.removeStuff();
        }

        this.bannedcommands = new ArrayList<String>();
        this.defaultRewards = new HashMap<Integer, ArrayList<String>>();
        this.maps = new HashMap<String, ParkourMap>();
        this.signs = new HashMap<org.bukkit.block.Sign, ParkourMap>();
        setupVault();
        readCommands();
        readConfig();
        readDefaultRewards();
        readParkourMaps();
    }
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length != 1) {
                sender.sendMessage("Usage: 'parkour reload' to reload ParkourCraft (will kick all players from Parkour maps)");
                return true;
            }
            reload();
            sender.sendMessage(this.defaultMessageColor + "ParkourCraft has been reloaded");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("parkour")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "invalid Parkour command. Usage is:");
                sender.sendMessage(ChatColor.RED + "'/parkour leave' to leave your parkour map");
                sender.sendMessage(ChatColor.RED + "'/parkour cp' to return to your last checkpoint/spawn");
                if (sender.hasPermission("parkourcraft.create")) sender.sendMessage(ChatColor.RED + "'/parkour reload' to reload ParkourCraft (will kick all players from Parkour maps)");
                return true;
            }
            Player player = (Player)sender;
            ParkourMap curmap = null;
            boolean inmap = false;
            for (ParkourMap pm : this.maps.values()) {
                if (pm == null)
                    continue;
                if (pm.containsPlayer(player)) {
                    inmap = true;
                    curmap = pm;
                    break;
                }
            }
            if ((args[0].equals("reload")) && (sender.hasPermission("perkourcraft.create"))) {
                reload();
                sender.sendMessage(ChatColor.RED + "ParkourCraft has been reloaded");
                getLogger().info("ParkourCraft reloaded by " + sender.getName());
                return true;
            }
            if (!inmap) {
                player.sendMessage(ChatColor.RED + "You are not currently playing parkour");
                return true;
            }
            if (args[0].equalsIgnoreCase("cp")) {
                curmap.sendToLastCP(player);
                return true;
            }if (args[0].equalsIgnoreCase("leave")) {
                curmap.removePlayer(player);
                curmap.players.remove(player);
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "invalid Parkour command. Usage is:");
        sender.sendMessage(ChatColor.RED + "'/parkour leave' to leave your parkour map");
        sender.sendMessage(ChatColor.RED + "'/parkour cp' to return to your last checkpoint/spawn");
        return true;
    }

    class SignListener implements Listener
    {

        @EventHandler
        public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
        {
            Player p = event.getPlayer();

            if (event.isCancelled()) return;
            boolean inMap = false;
            for (ParkourMap pkm : ParkourCraft.this.maps.values()) {
                if ((pkm == null) || 
                        (!pkm.containsPlayer(p))) continue;
                inMap = true;
                break;
            }

            if (!inMap) return;

            String base = event.getMessage().split(" ")[0];
            if (base.indexOf("/") == 0) base = base.substring(1);

            if (ParkourCraft.this.isAllowed(p, base)) {
                return;
            }

            event.setMessage("/");

            event.setCancelled(true);
            p.sendMessage(ChatColor.RED + "That command is not allowed while playing in parkour mode");
            p.sendMessage(ChatColor.RED + "Use /parkour leave to leave parkour mode");
        }
        @EventHandler(priority=EventPriority.HIGH)
        public void onSignRemove(BlockBreakEvent e) { if (e.isCancelled()) return;
        if ((!e.getBlock().getType().equals(Material.WALL_SIGN)) && (!e.getBlock().getType().equals(Material.SIGN_POST))) return;
        org.bukkit.block.Sign sign = (org.bukkit.block.Sign)e.getBlock().getState();
        if ((ParkourCraft.this.signs.containsKey(sign)) || (ParkourCraft.this.getSignType(sign) != ParkourCraft.SignType.NOTPARKOUR)) {
            if (!e.getPlayer().hasPermission("parkourcraft.create")) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to remove this ParkourCraft sign");
                return;
            }
            ParkourMap pks = ParkourCraft.this.signs.get(sign);
            if ((ParkourCraft.this.getSignType(sign).equals(ParkourCraft.SignType.INVALID)) || 
                    (ParkourCraft.this.getSignType(sign).equals(ParkourCraft.SignType.NOTPARKOUR))) return;
            e.getPlayer().sendMessage(ParkourCraft.this.defaultMessageColor + "ParkourCraft sign removed");

            if (pks == null) return;

            for (ParkourMap pm : ParkourCraft.this.maps.values()) {
                if (pm == null)
                    continue;
                pm.removeCheckPoint(pks.getLocation());
            }

            for (String s : ParkourCraft.this.maps.keySet()) {
                if (!s.equals(pks.getName())) continue; ParkourCraft.this.removeDataFor((ParkourMap)ParkourCraft.this.maps.get(s)); break;
            }

            ParkourCraft.this.maps.remove(pks.getName());
            ParkourCraft.this.maps.put(pks.getName(), null);
            pks.removeStuff();
            ParkourCraft.this.signs.remove(sign);
            ParkourCraft.this.saveParkourMaps();
        }
        }

        @EventHandler
        public void onSignClick(PlayerInteractEvent e)
        {
            int i;
            if ((e.getAction().equals(Action.LEFT_CLICK_BLOCK)) && (
                    (e.getClickedBlock().getType().equals(Material.WALL_SIGN)) || (e.getClickedBlock().getType().equals(Material.SIGN_POST)))) {
                org.bukkit.block.Sign sign = (org.bukkit.block.Sign)e.getClickedBlock().getState();
                if (!ParkourCraft.this.getSignType(sign).equals(ParkourCraft.SignType.START)) return;
                ParkourMap pkm = (ParkourMap)ParkourCraft.this.maps.get(sign.getLine(1));
                if (pkm == null) {
                    ParkourCraft.this.getLogger().info(ChatColor.RED + sign.getLine(1) + " has a start block up but isn't a created parkour map");
                    e.getPlayer().sendMessage(ChatColor.RED + "Sorry, this isn't connected to any ParkourCraft map. Please let your admins know");
                    return;
                }
                HashMap<Long, String> scores = pkm.getHighscores();
                TreeSet<Long> f = pkm.f;
                Player p = e.getPlayer();
                p.sendMessage(ParkourCraft.this.defaultMessageColor + "High Scores: ");
                i = 1;
                for (Long l : f) {
                    p.sendMessage(ParkourCraft.this.defaultMessageColor + i + ": " + l.longValue() / 1000.0D + " seconds by " + (String)scores.get(l));
                    i++;
                }
                return;
            }
            if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
            if ((!e.getClickedBlock().getType().equals(Material.WALL_SIGN)) && (!e.getClickedBlock().getType().equals(Material.SIGN_POST))) return;
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign)e.getClickedBlock().getState();
            ParkourCraft.SignType st = ParkourCraft.this.getSignType(sign);
            Player player = e.getPlayer();
            if ((st.equals(ParkourCraft.SignType.NOTPARKOUR)) || (st.equals(ParkourCraft.SignType.INVALID))) return;
            ParkourMap pkm = null;
            for (ParkourMap pm : ParkourCraft.this.maps.values()) {
                if ((pm == null) || 
                        (!pm.containsPlayer(player))) continue;
                pkm = pm;
                break;
            }

            if ((pkm == null) && (!st.equals(ParkourCraft.SignType.START))) {
                player.sendMessage(ChatColor.RED + "You are not currently playing parkour");
                return;
            }
            if ((st.equals(ParkourCraft.SignType.START)) && (pkm != null)) {
                player.sendMessage(ChatColor.RED + "You are already in a parkour map, you can't join another");
                player.sendMessage(ChatColor.RED + "use /parkour leave to leave if you are stuck in parkour mode");
                return;
            }
            if (st.equals(ParkourCraft.SignType.START)) {
                pkm = (ParkourMap)ParkourCraft.this.maps.get(sign.getLine(1));
                if (pkm == null) {
                    ParkourCraft.this.getLogger().info(sign.getLine(1) + " has a start block up but isn't a created parkour map");
                    e.getPlayer().sendMessage(ChatColor.RED + "Sorry, this isn't connected to any ParkourCraft map. Please let your admins know");
                    return;
                }
                if (!pkm.addPlayer(player, player.getLocation()))
                    player.sendMessage(ChatColor.RED + ParkourCraft.this.mvpplayermessage);
                else {
                    player.sendMessage(ParkourCraft.this.defaultMessageColor + "Parkour map " + pkm.getName() + " started");
                }
                return;
            }
            if (st.equals(ParkourCraft.SignType.END)) {
                ParkourMap pkm2 = (ParkourMap)ParkourCraft.this.maps.get(sign.getLine(1));
                if (pkm2 == null) {
                    ParkourCraft.this.getLogger().info(sign.getLine(1) + " has an end block up but isn't a created parkour map");
                    e.getPlayer().sendMessage(ChatColor.RED + "Sorry, this isn't connected to any ParkourCraft map. Please let your admins know");
                    return;
                }
                if (!pkm2.equals(pkm)) {
                    e.getPlayer().sendMessage(ChatColor.RED + "This is the end to " + pkm2.getName() + ". You are currently in " + pkm.getName());
                    return;
                }
                if (pkm.endPlayer(player)) {
                    ParkourCraft.this.saveHighScores(pkm);
                }
                return;
            }

            if ((st.equals(ParkourCraft.SignType.CHECKPOINT)) || (st.equals(ParkourCraft.SignType.SPAWNLOCATION))) {
                pkm.checkPoint(player, player.getLocation());
                player.sendMessage(ParkourCraft.this.defaultMessageColor + "Checkpoint reached");
                return;
            }
            if (st.equals(ParkourCraft.SignType.VIPCHECKPOINT)) {
                if (player.hasPermission("parkourcraft.vipcheckpoint")) {
                    pkm.checkPoint(player, player.getLocation());
                    player.sendMessage(ParkourCraft.this.defaultMessageColor + "Checkpoint reached");
                } else {
                    player.sendMessage(ChatColor.RED + ParkourCraft.this.vipcheckpointmessage);
                }
                return;
            }
        }

        @EventHandler(priority=EventPriority.HIGHEST)
        public void onSignCreate(SignChangeEvent e) {
            if (e.isCancelled()) return;
            if ((!e.getBlock().getType().equals(Material.WALL_SIGN)) && (!e.getBlock().getType().equals(Material.SIGN_POST))) return;
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign)e.getBlock().getState();

            for (int i = 0; i < e.getLines().length; i++) {
                sign.setLine(i, e.getLines()[i]);
            }
            if (!ParkourCraft.this.addSignFromPlayer(sign, e.getPlayer())) {
                e.setCancelled(true);
                e.getBlock().breakNaturally();
            } else {
                if ((ParkourCraft.this.getSignType(sign).equals(ParkourCraft.SignType.NOTPARKOUR)) || (ParkourCraft.this.getSignType(sign).equals(ParkourCraft.SignType.INVALID))) return;
                ParkourCraft.this.saveParkourMaps();
                e.getPlayer().sendMessage(ParkourCraft.this.defaultMessageColor + "ParkourCraft sign created");
            }
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent e) {
            for (ParkourMap pkm : ParkourCraft.this.maps.values()) {
                if ((pkm == null) || 
                        (!pkm.containsPlayer(e.getPlayer()))) continue;
                pkm.removePlayer(e.getPlayer());
                pkm.players.remove(e.getPlayer());
            }
        }

        @EventHandler(priority=EventPriority.HIGHEST)
        public void onPlayerRespawn(PlayerRespawnEvent e) {
            for (ParkourMap pkm : ParkourCraft.this.maps.values()) {
                if ((pkm == null) || 
                        (!pkm.containsPlayer(e.getPlayer()))) continue;
                if (ParkourCraft.this.deathBehavior == 0) {
                    e.setRespawnLocation((Location)pkm.curcp.get(e.getPlayer()));
                } else if (ParkourCraft.this.deathBehavior == 1) {
                    pkm.checkPoint(e.getPlayer(), pkm.getLocation());
                    e.setRespawnLocation((Location)pkm.curcp.get(e.getPlayer()));
                } else {
                    pkm.removePlayer(e.getPlayer());
                    pkm.players.remove(e.getPlayer());
                }
            }
        }
    }

    public static enum SignType
    {
        SPAWNLOCATION, CHECKPOINT, VIPCHECKPOINT, START, END, INVALID, NOTPARKOUR;
    }
}