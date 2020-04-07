package essentials.core.player;

import essentials.internal.CrashReport;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Packets;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;

import static essentials.Main.*;
import static essentials.PluginVars.serverIP;
import static mindustry.Vars.netServer;

public class PlayerCore {
    public boolean load(Player player, String... AccountID) {
        PlayerData playerData = playerDB.load(AccountID.length > 0 ? player.uuid : player.uuid, AccountID);
        if (playerData.error) {
            new CrashReport(new Exception("DATA NOT FOUND"));
            return false;
        }

        if (playerData.banned) {
            netServer.admins.banPlayerID(player.uuid);
            Call.onKick(player.con, Packets.KickReason.banned);
            return false;
        }

        String motd = tool.getMotd(playerData.locale);
        int count = motd.split("\r\n|\r|\n").length;
        if (count > 10) {
            Call.onInfoMessage(player.con, motd);
        } else {
            player.sendMessage(motd);
        }

        if (config.realname || config.passwordmethod.equals("discord")) player.name = playerData.name;
        if (playerData.colornick) colornick.targets.add(player);

        player.isAdmin = perm.isAdmin(player);

        playerData.uuid(player.uuid);
        playerData.connected(true);
        playerData.lastdate(tool.getTime());
        playerData.connserver(serverIP);
        playerData.exp(playerData.exp + playerData.joincount);
        playerData.joincount(playerData.joincount++);
        playerData.login(true);
        return true;
    }

    public PlayerData NewData(String name, String uuid, String country, String country_code, String language, boolean connected, String connserver, String permission, Long udid, String email, String accountid, String accountpw) {
        return new PlayerData(
                name,
                uuid,
                country,
                country_code,
                language,
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                "0/0",
                tool.getTime(),
                tool.getTime(),
                "none",
                "none",
                "",
                "00:00:00",
                0,
                0,
                0,
                0,
                0,
                "00:00:00",
                "none",
                false,
                false,
                true,
                false,
                connected,
                connserver,
                permission,
                false,
                true,
                udid,
                email,
                accountid,
                accountpw
        );
    }

    public boolean isLocal(Player player) {
        try {
            InetAddress addr = InetAddress.getByName(netServer.admins.getInfo(player.uuid).lastIP);
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) return true;
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean login(Player player, String id, String pw) {
        try {
            PreparedStatement pstmt = database.conn.prepareStatement("SELECT * from players WHERE accountid=? AND accountpw=?");
            pstmt.setString(1, id);
            pstmt.setString(2, pw);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            new CrashReport(e);
            return false;
        }
    }

    public void tempban(Player player, LocalTime time, String reason) {
        PlayerData playerData = playerDB.get(player.uuid);
        playerData.bantimeset(time.toString());
    }
}