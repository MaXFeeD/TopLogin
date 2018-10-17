package cn.guestc.nukkit.login.events;

import cn.guestc.nukkit.login.TopLogin;
import cn.guestc.nukkit.login.TopLoginAPI;
import cn.guestc.nukkit.login.utils.UserData;
import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.*;

import java.util.HashMap;

public class RegisterEvent implements Listener {
    private TopLogin plugin;

    private TopLoginAPI API;

    public enum RegisterState{
        confirmName,
        Passwd,
        confirmPasswd,
        Mail
    }

    private HashMap<String,RegisterState> registers = new HashMap<>();

    private HashMap<String, UserData> reging = new HashMap<>();

    public RegisterEvent(TopLogin toplogin){
        plugin = toplogin;
        API = plugin.api;
    }

    @EventHandler(priority = EventPriority.HIGH,ignoreCancelled = false)
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        String name = player.getName();
        if(!plugin.dataHelper.IsRegister(name)){
            API.Message(player,API.getMessage("reg-comfirm-name"));
        }else{
            API.Message(player,API.getMessage("login-in-message"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH,ignoreCancelled = false)
    public void onQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        String name = player.getName();
        API.LoginOut(name);
        if(registers.containsKey(name)){
            registers.remove(name);
        }
        if(reging.containsKey(name)){
            reging.remove(name);
        }
    }

    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=false)
    public void onChat(PlayerChatEvent event){
        Player player = event.getPlayer();
        String name = player.getName();
        if(registers.containsKey(name)){
            String msg = event.getMessage();
            event.setCancelled(true);
            switch(registers.get(name)){
                case confirmName:
                    if(msg.toLowerCase().equals(name.toLowerCase())){
                        registers.put(name,RegisterState.Passwd);
                        API.Message(player,API.getMessage("reg-passwd"));
                        reging.put(name,new UserData());
                        return;
                    }
                    API.Message(player,API.getMessage("reg-comfirm-name-wrong"));
                    break;
                case Mail:
                    if(!TopLoginAPI.isMail(msg)){
                        API.Message(player,API.getMessage("reg-mail-wrong"));
                        API.Message(player,API.getMessage("reg-mail"));
                        return;
                    }
                    UserData ud1 = reging.get(name);
                    String passwd = TopLoginAPI.getPasswdFormStr(ud1.passwd);
                    plugin.dataHelper.AddUser(name,passwd,msg);
                    API.Message(player,API.getMessage("reg-success"));
                    API.Message(player, String.format(API.getMessage("reg-success"),name,ud1.passwd,msg));
                    API.LoginIn(name);
                    reging.remove(name);
                    registers.remove(name);
                    break;
                case Passwd:
                    String remsg = API.CheckPasswd(msg);
                    if(remsg != null){
                        API.Message(player,remsg);
                        API.Message(player,API.getMessage("reg-comfirm-name-wrong"));
                        return;
                    }
                    API.Message(player,API.getMessage("reg-passwd-comfirm"));
                    registers.put(name,RegisterState.confirmPasswd);
                    UserData ud = reging.get(name);
                    ud.name = name;
                    ud.passwd = msg;
                    reging.put(name,ud);
                    break;
                case confirmPasswd:
                    if(!reging.get(name).passwd.equals(msg)){
                        API.Message(player,API.getMessage("reg-passwd-comfirm-not"));
                        API.Message(player,API.getMessage("reg-passwd-comfirm"));
                        return;
                    }
                    API.Message(player,API.getMessage("reg-mail"));
                    registers.put(name,RegisterState.Mail);
                    break;
                default:

                    break;
            }
        }else{
                if(!API.cdata.UnloginChat){
                    if(!API.isLogin(name)){
                        event.setCancelled(true);
                    }
                }
        }


    }

    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=false)
    public void PreLogin(PlayerPreLoginEvent event){
        if(!event.isCancelled()){
            String name = event.getPlayer().getName();
            if(!plugin.dataHelper.IsRegister(name)){
                String msg = null;
                if(API.isBanReg(name)){
                    msg = API.getMessage("reg-comfirm-name-error");
                }
                if(name.length() > API.cdata.UserMaxLen){
                    msg = API.getMessage("reg-comfirm-name-max-lenght");
                }
                if(name.length() < API.cdata.UserMinLen){
                    msg = API.getMessage("reg-comfirm-name-min-lenght");
                }
                if(msg != null){
                    event.setKickMessage(msg);
                    event.setCancelled(true);
                    return;
                }
                registers.put(name,RegisterState.confirmName);
            }
        }
    }
}
