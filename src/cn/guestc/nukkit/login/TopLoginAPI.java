package cn.guestc.nukkit.login;

import cn.guestc.nukkit.login.Config.MysqlConfig;
import cn.guestc.nukkit.login.utils.ConfigData;
import cn.nukkit.Player;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.network.protocol.TextPacket;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.LoginChainData;

import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TopLoginAPI {

    private static TopLoginAPI obj;

    private TopLogin plugin;

    private ArrayList<String> loginusers = new ArrayList<>();

    private Map<String,Object> language;

    protected ArrayList<String> banusers;

    public ConfigData cdata;

    private FormWindow Funlogin;
    private FormWindow Floginin;


    public static TopLoginAPI getObject(){
        return TopLoginAPI.obj;
    }

     protected TopLoginAPI(TopLogin toplogin){
        plugin = toplogin;
        TopLoginAPI.obj = this;
         Funlogin = new FormWindowSimple(getMessage("login-usage-ui-title"),getMessage("login-usage-ui-text"));
         Floginin = new FormWindowSimple(getMessage("login-in-ui-title"),getMessage("login-in-ui-text"));
    }

    protected void init(){
        language = plugin.getLanuage();
        banusers = (ArrayList<String>) plugin.pconfig.get("ban-username");
        cdata = new ConfigData();
        cdata.UserMinLen = UserMin();
        cdata.UserMaxLen = UserMax();
        cdata.PasswdMaxLen = PasswdMax();
        cdata.PasswdMinLen = PasswdMin();
        cdata.MessageType = Byte.parseByte(plugin.pconfig.get("message-type").toString());
        cdata.UnloginMove = Boolean.parseBoolean(plugin.pconfig.get("unlogin-move").toString());
        cdata.UnloginMessage = Boolean.parseBoolean(plugin.pconfig.get("unlogin-message").toString());
        cdata.UnloginBreak = Boolean.parseBoolean(plugin.pconfig.get("unlogin-break").toString());
        cdata.UnloginCraft = Boolean.parseBoolean(plugin.pconfig.get("unlogin-craft").toString());
        cdata.UnloginPlace = Boolean.parseBoolean(plugin.pconfig.get("unlogin-place").toString());
        cdata.UnloginInteract = Boolean.parseBoolean(plugin.pconfig.get("unlogin-interact").toString());
        cdata.UnloginDropItem = Boolean.parseBoolean(plugin.pconfig.get("unlogin-dropitem").toString());
        cdata.UnloginPickItem = Boolean.parseBoolean(plugin.pconfig.get("unlogin-pickitem").toString());
        cdata.AutoLogin = Boolean.parseBoolean(plugin.pconfig.get("autologin").toString());
        cdata.AutoLoginValidHours = Integer.parseInt(plugin.pconfig.get("autologin-valid-hours").toString());
        cdata.LoginType = plugin.pconfig.get("login-type").toString();
        cdata.MultiServer = Boolean.parseBoolean(plugin.pconfig.get("multi-server").toString());
        cdata.MainServer = Boolean.parseBoolean(plugin.pconfig.get("main-server").toString());
        cdata.EnableFormUI = Boolean.parseBoolean(plugin.pconfig.get("enable-form-ui").toString());
    }

    public static String getPasswdFormStr(String str){
        byte[] bts;
        try{
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            bts = md5.digest(str.getBytes());
        }catch (Exception e){
            return null;
        }
        final char[] hex_d = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < bts.length;i++){
            sb.append(hex_d[(bts[i] >> 4) & 0x0f]);
            sb.append(hex_d[bts[i] & 0x0f]);
        }
        return sb.toString();
    }

    public static String getTime(){
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sd.format(new Date());
    }

    public static Date getTime(String date) throws ParseException {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sd.parse(date);
    }

    public void LoginIn(Player player){
        String name = player.getName();
        if(!loginusers.contains(name)){
            loginusers.add(name);
            plugin.getLogger().info(String.format(getMessage("user-login-message"),name,getNameFromIp(player.getAddress())));
            if(cdata.EnableFormUI){
                player.showFormWindow(Floginin);
            }
        }
    }

    public void LoginOut(Player player){
        String name = player.getName();
        if(loginusers.contains(name)){
            loginusers.remove(name);
            plugin.dataHelper.LoginOut(player);
        }
    }

    public void AutoLogin(Player player){
        String user = player.getName();
        if(cdata.AutoLogin){
            Date ltime = plugin.dataHelper.getLastTime(user);
            if(ltime != null){
                Date ntime = new Date();
                if((ntime.getTime() - ltime.getTime()) <= (1000*60*60* cdata.AutoLoginValidHours)){
                    LoginChainData data = player.getLoginChainData();
                    if(data.getClientId() == plugin.dataHelper.getCid(user) && data.getClientUUID().toString().equals(plugin.dataHelper.getUUID(user))){
                        Message(player,String.format(getMessage("autologin"),cdata.AutoLoginValidHours));
                        LoginIn(player);
                    }
                    return;
                }
            }
        }
        if(cdata.EnableFormUI){
            player.showFormWindow(Funlogin);
        }
        Message(player,getMessage("login-in-message"));
    }

    public boolean isLogin(String user){
        if(loginusers.contains(user))return true;
        if(cdata.MultiServer){
            MysqlConfig mc = (MysqlConfig) plugin.dataHelper;
            return mc.isLogin(user);
        }
        return false;
    }

    public void Message(Player player,String msg){
        if(!cdata.UnloginMessage){
            TextPacket pk = new TextPacket();
            pk.type = cdata.MessageType;
            pk.offset = 600;
            pk.message = msg;
            player.dataPacket(pk);
            return;
        }
        player.sendMessage(msg);
    }

    public String getMessage(String str){
        if(language.containsKey(str)){
            return language.get(str).toString();
        }
        return null;
    }

    public int UserMin(){
        if(plugin.pconfig.exists("username-min")){
            return Integer.parseInt(plugin.pconfig.get("username-min").toString());
        }
        return 3;
    }

    public int UserMax(){
        if(plugin.pconfig.exists("username-max")){
            return Integer.parseInt(plugin.pconfig.get("username-max").toString());
        }
        return 16;
    }

    public int PasswdMin(){
        if(plugin.pconfig.exists("passwd-min")){
            return Integer.parseInt(plugin.pconfig.get("passwd-min").toString());
        }
        return 8;
    }

    public int PasswdMax(){
        if(plugin.pconfig.exists("passwd-max")){
            return Integer.parseInt(plugin.pconfig.get("passwd-max").toString());
        }
        return 16;
    }

    public boolean isBanReg(String user){
        if(banusers.contains(user.toLowerCase()))return true;
        for (String u:banusers) {
            if(Pattern.matches(u,user))return true;
        }
        return false;
    }

    public static boolean isMail(String str){
        String pattern = "^\\w{2,}@([A-Za-z0-9]{2,}(\\.[A-Za-z0-9]{2,})+)$";
        return Pattern.matches(pattern,str);
    }

    public static String ArrayToString(String[] args){
        StringBuilder sb = new StringBuilder();
        for (String arg : args){
            sb.append(arg);
        }
        return sb.toString();
    }

    public String CheckPasswd(String passwd){
        String remsg =null;
        if(passwd.length() > cdata.PasswdMaxLen){
            remsg = getMessage("reg-passwd-max-lenght");
        }
        if(passwd.length() < cdata.PasswdMinLen){
            remsg = getMessage("reg-passwd-min-lenght");
        }
        return remsg;
    }

    public static String getNameFromIp(String ip){
        //todo
        return ip;
    }

    public ArrayList<String> getLoginUsers(){
        return loginusers;
    }




}
