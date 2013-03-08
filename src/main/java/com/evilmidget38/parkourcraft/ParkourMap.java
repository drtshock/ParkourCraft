package com.evilmidget38.parkourcraft;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ParkourMap
{
    Location loc;
    int size;
    String name;
    int numhighscores;
    Economy econ;
    boolean hasEconomy = false;
    HashMap<Player, Location> curcp;
    HashMap<Player, Location> fromloc;
    HashMap<Long, String> highscores;
    HashMap<Player, Long> starttimes;
    HashMap<Integer, ArrayList<String>> rewards;
    TreeSet<Long> f = new TreeSet<Long>();
    ArrayList<Player> players;

    public Location getLocation()
    {
        return this.loc;
    }
    public void removeStuff() {
        removeAll();
    }

    public void setEconomy(Economy e) {
        if (e != null) {
            this.hasEconomy = true;
            this.econ = e;
        }
    }
    public String getName() {
        return this.name;
    }
    private void init() { 
        this.players = new ArrayList<Player>();
        this.curcp = new HashMap<Player, Location>();
        this.fromloc = new HashMap<Player, Location>();
    }

    public void setHighScores(HashMap<Long, String> highs)
    {
        this.highscores = highs;
        if (highs == null) this.highscores = new HashMap<Long, String>();
        this.f.clear();
        this.f.addAll(this.highscores.keySet());
        while (this.f.size() > this.numhighscores) {
            long l = ((Long)this.f.last()).longValue();
            this.f.remove(Long.valueOf(l));
            this.highscores.remove(Long.valueOf(l));
        }
    }

    public void setRewards(HashMap<Integer, ArrayList<String>> rewa) {
        this.rewards = rewa;
        if (this.rewards == null) this.rewards = new HashMap<Integer, ArrayList<String>>();
        for (ArrayList<String> rwd : this.rewards.values()) {
            Iterator<String> i = rwd.iterator();
            while (i.hasNext())
            {
                String s = (String)i.next();
                if ((s != null) && (!s.isEmpty()))
                    continue;
                i.remove();
            }
        }
    }

    public HashMap<Integer, ArrayList<String>> getRewards() {
        return this.rewards;
    }
    public ParkourMap(int Size, String Name, Location Spawn, int numhighscores) { Spawn.add(0.5D, 0.0D, 0.5D);
    this.loc = Spawn;
    Location l = this.loc.getBlock().getLocation();
    l.setYaw(this.loc.getYaw());
    this.loc = l;
    this.numhighscores = numhighscores;
    if (this.rewards == null) this.rewards = new HashMap<Integer, ArrayList<String>>();
    setHighScores(null);
    this.starttimes = new HashMap<Player, Long>();
    this.name = Name;
    this.size = Size;
    init(); }

    public void checkPoint(Player p, Location cp) {
        p.setHealth(20);
        p.setFoodLevel(20);
        this.curcp.put(p, cp);
    }
    public boolean containsPlayer(Player p) {
        return this.players.contains(p);
    }
    public void sendToLastCP(Player p) {
        if (this.curcp.containsKey(p))
            p.teleport((Location)this.curcp.get(p));
        else {
            p.teleport(getLocation());
        }
        p.sendMessage("You were sent to your last checkpoint");
    }
    public boolean addPlayer(Player newplay, Location fromLocation) {
        if ((newplay.hasPermission("parkour.overrideplayerlimit")) || (this.players.size() < this.size)) {
            for (Player p : this.players) {
                p.hidePlayer(newplay);
                newplay.hidePlayer(p);
            }
            this.players.add(newplay);

            Location l = this.loc.getBlock().getLocation();
            l.setYaw(this.loc.getYaw());
            l.setX(this.loc.getX() + 0.5D);
            l.setZ(this.loc.getZ() + 0.5D);
            this.curcp.put(newplay, l);
            this.fromloc.put(newplay, fromLocation);
            this.starttimes.put(newplay, Long.valueOf(System.currentTimeMillis()));

            newplay.teleport((Location)this.curcp.get(newplay));
            newplay.setFoodLevel(20);
            return true;
        }
        return false;
    }
    public void removeAll() {
        for (Player p : this.players) {
            removePlayer(p);
        }
        this.players = null;
        this.fromloc = null;
        this.curcp = null;
    }
    public void removeCheckPoint(Location cp) {
        for (Player p : this.curcp.keySet()) {
            if (!((Location)this.curcp.get(p)).equals(cp)) continue; this.curcp.put(p, this.loc);
        }
    }

    public int addHighScore(String player, long l) {
        if ((!this.f.isEmpty()) && (this.f.size() >= this.numhighscores) && (((Long)this.f.last()).longValue() < l)) return -1;
        this.f.add(Long.valueOf(l));
        this.highscores.put(Long.valueOf(l), player);
        if (this.f.size() > this.numhighscores) {
            this.highscores.remove(this.f.last());
            this.f.remove(this.f.last());
        }
        Iterator<Long> i = this.f.iterator();

        int place = 1;
        while (i.hasNext()) {
            long k = ((Long)i.next()).longValue();
            if (k == l) {
                return place;
            }
            place++;
        }

        return -1;
    }
    public boolean endPlayer(Player end) {
        long l = removePlayer(end);
        this.players.remove(end);
        if (l == -1L) return false;
        double d = l / 1000.0D;
        end.sendMessage("You took " + d + " seconds to finish the course");
        int place = addHighScore(end.getName(), l);
        rewardPlayer(end, place);
        return place != -1;
    }

    public void rewardPlayer(Player end, int place) {
        //TODO: Improve the cleaning in this method, and remove repetitive lookups.
        if (!this.rewards.containsKey(Integer.valueOf(place))) this.rewards.put(Integer.valueOf(place), new ArrayList<String>());
        ArrayList<String> editrewlist = (ArrayList<String>)this.rewards.get(Integer.valueOf(place));
        int i = place % 10;
        String modifierst = "th";
        switch (i) { case 1:
            modifierst = "st";
            break;
        case 2:
            modifierst = "nd";
            break;
        case 3:
            modifierst = "rd";
        }

        if (place == -1) editrewlist = (ArrayList<String>)this.rewards.get(Integer.valueOf(0));

        if (((editrewlist == null) || (editrewlist.isEmpty())) && (place != -1)) {
            end.sendMessage("There is no reward for " + place + modifierst + " place");
            if ((this.rewards.get(Integer.valueOf(0)) == null) || (((ArrayList<String>)this.rewards.get(Integer.valueOf(0))).isEmpty())) return;
            editrewlist = (ArrayList<String>)this.rewards.get(Integer.valueOf(0));
            end.sendMessage("You did still get a reward for just finishing the map"); } else {
                if (((editrewlist == null) || (editrewlist.isEmpty())) && (place == -1)) {
                    end.sendMessage("There is no reward for just finishing the map");
                    return;
                }if ((editrewlist != null) && (!editrewlist.isEmpty()) && (place == -1))
                    end.sendMessage("You didn't get a high score, but still won a prize");
                else if ((editrewlist != null) && (!editrewlist.isEmpty()))
                    end.sendMessage("You got " + place + modifierst + " place");
            }
        for (String editrew : editrewlist)
            if (editrew.startsWith("VAULT")) {
                if (this.hasEconomy) {
                    String[] args = editrew.split(" ");
                    try {
                        EconomyResponse er = this.econ.depositPlayer(end.getName(), Double.parseDouble(args[1]));
                        if (er.type.equals(EconomyResponse.ResponseType.NOT_IMPLEMENTED)) {
                            System.out.println("There was an issue with vault. Do you have a compatible economy set up?"); continue;
                        }if (!er.type.equals(EconomyResponse.ResponseType.FAILURE)) continue;
                        System.out.println("Vault failed to pay player " + end.getName() + " " + args[1] + " money/coins");
                    }
                    catch (Exception e) {
                        if (place < 0) place++;
                        System.out.println("rewards for " + getName() + " and place number " + place + " are not configured correctly. check the " + getName() + ".yml file for errors");
                        return;
                    }
                } else {
                    System.out.println("did not get an economy, yet vault is set as a reward. is vault installed?");
                }
            } else if (editrew.startsWith("MSGPLAYER")) {
                editrew.trim();
                editrew = editrew.substring(editrew.indexOf("MSGPLAYER") + 9);
                editrew = editrew.trim();
                end.sendMessage(editrew);
            } else {
                if (editrew.contains("&p")) editrew = editrew.replaceAll("&p", end.getName());
                if (editrew.contains("&m")) editrew = editrew.replaceAll("&m", getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), editrew);
            }
    }

    public long removePlayer(Player remove) {
        long l = System.currentTimeMillis();
        if (this.fromloc.containsKey(remove))
            remove.teleport((Location)this.fromloc.get(remove));
        else {
            remove.teleport(remove.getWorld().getSpawnLocation());
        }
        for (Player p : this.players) {
            p.showPlayer(remove);
            remove.showPlayer(p);
        }
        remove.sendMessage("You are no longer in parkour mode");
        if (!this.starttimes.containsKey(remove)) return -1L;
        return l - ((Long)this.starttimes.get(remove)).longValue();
    }

    public HashMap<Long, String> getHighscores() {
        return this.highscores;
    }
}