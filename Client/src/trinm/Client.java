package trinm;

import java.io.*;
import java.net.*;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class Client implements Runnable {
    public Socket server;
    public ClientUI ui;
    public ObjectInputStream In;
    public ObjectOutputStream Out;
    
    public Client(ClientUI frame) throws IOException {
        ui = frame;
        server = new Socket("192.168.1.5", 9999);
        Out = new ObjectOutputStream(server.getOutputStream());
        Out.flush();
        In = new ObjectInputStream(server.getInputStream());
        ui.user = new User[50];
        ui.group = new Group[50];
    }
    
    @Override
    public void run() {
        while(true){
            try{
                Data msg = (Data) In.readObject();
                
                if(msg.type.equals("login")){
                    if(msg.content.equals("wellcome")){
                        ui.jPanel1.setVisible(false);
                        ui.jPanel3.setVisible(true);
                        ui.setSize(905, 630);
                        ui.lName.setText(msg.recipient1);
                        ui.setTitle(msg.recipient1);
                        ui.reloadPage();
                        
                        ui.user[ui.userCount] = new User("SERVER");
                        ui.userCount++;
                        
                        ui.group[ui.groupCount] = new Group("All");
                        ui.groupCount++;
                    }
                    else if(msg.content.equals("exist")){
                        ui.messageErrorForLogin("Tài khoản đang được sử dụng.");
                    }
                    else if(msg.content.equals("user")){
                        ui.messageErrorForLogin("Tài khoản không tồn tại.");
                    }
                    else if(msg.content.equals("pass")){
                        ui.messageErrorForLogin("Mật khẩu bạn nhập không đúng.");
                    }
                    else if(msg.content.equals("error")){
                        JOptionPane.showMessageDialog(ui, "Quá trình đăng nhập xảy ra lỗi. Vui lòng đăng nhập lại.");
                    }
                }
                
                else if(msg.type.equals("register")){
                    if(msg.content.equals("true")){
                        JOptionPane.showMessageDialog(ui, "Đăng ký tài khoản thành công. Quay lại trang đăng nhập.");
                        ui.setPageLoginFromRegister();
                    }
                    else if(msg.content.equals("false")){
                        ui.messageErrorForRegister("Username đã tồn tại.");
                    }
                }
                else if(msg.type.equals("newuser")) {
                    if(!msg.content.equals(ui.lName.getText())){
                        ui.defaultListModelContact.addElement(msg.content);
                        ui.user[ui.userCount] = new User(msg.content);
                        ui.cbFriend[ui.userCount] = new JCheckBox(msg.content);
                        ui.jPanel10.add(ui.cbFriend[ui.userCount]);
                        ui.userCount++;
                    }
                }
                else if(msg.type.equals("exit")){
                    ui.defaultListModelContact.removeElement(msg.sender);
                    ui.jPanel10.remove(ui.cbFriend[findUserCount(msg.sender)]);
                    removeUser(msg.sender);
                    exitGroup(msg.sender);
                }
                else if(msg.type.equals("message")){
                    new PlayAlarm();
                    findUser(msg.sender).addChat(msg.sender+": "+msg.content+"\n");
                    ui.lContacts.setSelectedValue(msg.sender, true);
                    ui.taContent.setText(findUser(msg.sender).chat);
                    ui.jTabbedPane1.setSelectedIndex(0);
                    ui.jLabel1.setVisible(true);
                    ui.jLabel1.setText("Online");
                    ui.taContent.setText(findUser(msg.sender).chat);
                    ui.lNameFriend.setText(msg.sender);
                }
                else if(msg.type.equals("remove")) {
                    JOptionPane.showMessageDialog(ui, "Mất kết nối với máy chủ.");
                    System.exit(0);
                }
                else if(msg.type.equals("newgroup")){
                    ui.defaultListModelGroup.addElement(msg.content);
                    ui.group[ui.groupCount] = new Group(msg.content);
                    if(!msg.recipient1.isEmpty()){
                        ui.group[ui.groupCount].member1 = msg.recipient1;
                        if(!msg.recipient2.isEmpty()){
                            ui.group[ui.groupCount].member2 = msg.recipient2;
                            if(!msg.recipient3.isEmpty()){
                                ui.group[ui.groupCount].member3 = msg.recipient3;
                                if(!msg.recipient4.isEmpty()){
                                    ui.group[ui.groupCount].member4 = msg.recipient4;
                                }
                            }
                        }
                    }
                    ui.groupCount++;
                }
                else if(msg.type.equals("messagegroup")){
                    new PlayAlarm();
//                    if(!msg.sender.equals(ui.lName)){
                    findGroup(msg.recipient1).addChat(msg.sender+": "+msg.content+"\n");
                    ui.lGroup.setSelectedValue(msg.recipient1, true);
//                    }
                    ui.jTabbedPane1.setSelectedIndex(1);
                    ui.jLabel1.setVisible(true);
                    ui.jLabel1.setText(ui.lName.getText()+findGroup(msg.recipient1).toString());
                    ui.taContent.setText(findGroup(msg.recipient1).chat);
                    ui.lNameFriend.setText(msg.recipient1);
                }
                else if(msg.type.equals("sendfile")){
                    findUser(msg.sender).addChat(msg.sender+": Đã tải lên tập tin '"+msg.content+"'\n");
                    ui.lContacts.setSelectedValue(msg.sender, true);
                    ui.taContent.setText(findUser(msg.sender).chat);
                    ui.jTabbedPane1.setSelectedIndex(0);
                    ui.jLabel1.setVisible(true);
                    ui.jLabel1.setText("Online");
                    ui.taContent.setText(findUser(msg.sender).chat);
                    ui.lNameFriend.setText(msg.sender);
                    
                    if(JOptionPane.showConfirmDialog(ui, ("Chấp nhận tập tin '"+msg.content+"' từ "+msg.sender+" không?")) == 0){
                        JFileChooser jf = new JFileChooser();
                        jf.setSelectedFile(new File(msg.content));
                        int returnVal = jf.showSaveDialog(ui);
                       
                        String saveTo = jf.getSelectedFile().getPath();
                        if(saveTo != null && returnVal == JFileChooser.APPROVE_OPTION){
                            Download dwn = new Download(saveTo, ui);
                            Thread t = new Thread(dwn);
                            t.start();
                            send(new Data("repsendfile", msg.recipient1, (""+dwn.port), msg.sender, msg.content));
                            findUser(msg.sender).addChat(msg.recipient1+": Đã tải xuống tập tin '"+msg.content+"'\n");
                        }
                        else{
                            findUser(msg.sender).addChat(msg.recipient1+": Đã từ chối tập tin '"+msg.content+"'\n");
                            send(new Data("repsendfile", msg.recipient1, "NO", msg.sender, msg.content));
                        }
                    }
                    else{
                        findUser(msg.sender).addChat(msg.recipient1+": Đã từ chối tập tin '"+msg.content+"'\n");
                        send(new Data("repsendfile", msg.recipient1, "NO", msg.sender, msg.content));
                    }
                    ui.taContent.setText(findUser(msg.sender).chat);
                }
                else if(msg.type.equals("repsendfile")){
                    if(!msg.content.equals("NO")){
                        int port  = Integer.parseInt(msg.content);
                        String addr = msg.sender;
                        
                        Upload upl = new Upload(addr, port, ui.file, ui);
                        Thread t = new Thread(upl);
                        t.start();
                        findUser(msg.recipient3).addChat(msg.recipient3+": Đã tải xuống tập tin"+msg.recipient2+"\n");
                    }
                    else {
                        findUser(msg.recipient3).addChat(msg.recipient3+": Đã từ chối tập tin"+msg.recipient2+"\n");
                    }
                    
                    ui.lContacts.setSelectedValue(msg.recipient3, true);
                    ui.jTabbedPane1.setSelectedIndex(0);
                    ui.jLabel1.setVisible(true);
                    ui.jLabel1.setText("Online");
                    ui.taContent.setText(findUser(msg.recipient3).chat);
                    ui.lNameFriend.setText(msg.sender);
                }
                else if(msg.type.equals("leavegroup")){
                    Group tempp = findGroup(msg.content);
                    if(tempp.member1.equals(msg.sender)){
                        tempp.member1 = tempp.member2;
                        tempp.member2 = tempp.member3;
                        tempp.member3 = tempp.member4;
                        tempp.member4 = "";
                    }
                    if(tempp.member2.equals(msg.sender)){
                        tempp.member2 = tempp.member3;
                        tempp.member3 = tempp.member4;
                        tempp.member4 = "";
                    }
                    if(tempp.member3.equals(msg.sender)){
                        tempp.member3 = tempp.member4;
                        tempp.member4 = "";
                    }
                    if(tempp.member4.equals(msg.sender)){
                        tempp.member4 = "";
                    }
                    
                    findGroup(msg.content).addChat(msg.sender+": đã rời nhóm.\n");
                    ui.lGroup.setSelectedValue(msg.content, true);
                    
                    ui.jTabbedPane1.setSelectedIndex(1);
                    ui.jLabel1.setVisible(true);
                    ui.jLabel1.setText(ui.lName.getText()+findGroup(msg.content).toString());
                    ui.taContent.setText(findGroup(msg.content).chat);
                    ui.lNameFriend.setText(msg.content);
                }
                
                else if(msg.type.equals("addmember")){
                    Group tempp = findGroup(msg.content);
                    if(tempp.member1.isEmpty()){
                        tempp.member1 = msg.recipient1;
                    }
                    else if(tempp.member2.isEmpty()){
                        tempp.member2 = msg.recipient1;
                    }
                    else if(tempp.member3.isEmpty()){
                        tempp.member3 = msg.recipient1;
                    }
                    else if(tempp.member4.isEmpty()){
                        tempp.member4 = msg.recipient1;
                    }
                    
                    findGroup(msg.content).addChat(msg.sender+": đã thêm "+msg.recipient1+" vào nhóm.\n");
                    ui.lGroup.setSelectedValue(msg.content, true);
                    
                    ui.jTabbedPane1.setSelectedIndex(1);
                    ui.jLabel1.setVisible(true);
                    ui.jLabel1.setText(ui.lName.getText()+findGroup(msg.content).toString());
                    ui.taContent.setText(findGroup(msg.content).chat);
                    ui.lNameFriend.setText(msg.content);
                }
//                else if(msg.type.equals("sendfilegroup")){
//                    findGroup(msg.recipient1).chat += msg.sender+": "+msg.content+"\n";
//                    ui.lGroup.setSelectedValue(msg.recipient1, true);
//                    ui.taContent.setText(findGroup(msg.recipient1).chat);
//                    ui.jTabbedPane1.setSelectedIndex(1);
//                    ui.jLabel1.setVisible(true);
//                    ui.jLabel1.setText(findGroup(msg.recipient1).toString());
//                    ui.taContent.setText(findGroup(msg.recipient1).chat);
//                    ui.lNameFriend.setText(msg.recipient1);
                    
//                    if(JOptionPane.showConfirmDialog(ui, ("Chấp nhận '"+msg.content+"' từ "+msg.sender+" không?")) == 0){
//                        
//                        JFileChooser jf = new JFileChooser();
//                        jf.setSelectedFile(new File(msg.content));
//                        int returnVal = jf.showSaveDialog(ui);
//                       
//                        String saveTo = jf.getSelectedFile().getPath();
//                        if(saveTo != null && returnVal == JFileChooser.APPROVE_OPTION){
//                            Download dwn = new Download(saveTo, ui);
//                            Thread t = new Thread(dwn);
//                            t.start();
//                        }
//                    }
//                }
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void send(Data msg){
        try {
            Out.writeObject(msg);
            Out.flush();
        } 
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public User findUser(String username){
        for(int i = 0; i < ui.userCount; i++){
            if(ui.user[i].username.equals(username)){
                return ui.user[i];
            }
        }
        return null;
    }
    
    public int findUserCount (String username){
        for(int i = 0; i < ui.userCount; i++){
            if(ui.user[i].username.equals(username)){
                return i;
            }
        }
        return 0;
    }
    
    public void removeUser (String username){
        int pos = findUserCount(username);
        if (pos >= 1){
	    if (pos < ui.userCount-1){
                for (int i = pos+1; i < ui.userCount; i++){
                    ui.user[i-1] = ui.user[i];
	        }
	    }
	    ui.userCount--;
	}
    }
    
    public Group findGroup(String groupname) {
        for(int i = 0; i < ui.groupCount; i++) {
            if(ui.group[i].groupName.equals(groupname)) {
                return ui.group[i];
            }
        }
        return null;
    }
    
    public int findGroupCount (String groupname){
        for(int i = 0; i < ui.groupCount; i++){
            if(ui.group[i].groupName.equals(groupname)){
                return i;
            }
        }
        return 0;
    }
    
    public void removeGroup (String groupname){
        int pos = findGroupCount(groupname);
        if (pos >= 1){
	    if (pos < ui.groupCount-1){
                for (int i = pos+1; i < ui.groupCount; i++){
                    ui.group[i-1] = ui.group[i];
	        }
	    }
	    ui.groupCount--;
	}
    }
    
    public void exitGroup (String member){
        for(int i = 0; i < ui.groupCount; i++){
            if(ui.group[i].member1.equals(member)){
                ui.group[i].addChat(member+": đã thoát.\n");
                ui.group[i].member1 = ui.group[i].member2;
                ui.group[i].member2 = ui.group[i].member3;
                ui.group[i].member3 = ui.group[i].member4;
                ui.group[i].member4 = "";
            }
            else if(ui.group[i].member2.equals(member)){
                ui.group[i].addChat(member+": đã thoát.\n");
                ui.group[i].member2 = ui.group[i].member3;
                ui.group[i].member3 = ui.group[i].member4;
                ui.group[i].member4 = "";
            }
            else if(ui.group[i].member3.equals(member)){
                ui.group[i].addChat(member+": đã thoát.\n");
                ui.group[i].member3 = ui.group[i].member4;
                ui.group[i].member4 = "";
            }
            else if(ui.group[i].member4.equals(member)){
                ui.group[i].addChat(member+": đã thoát.\n");
                ui.group[i].member4 = "";
            }
        }
    }
}