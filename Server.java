/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server2;

import java.io.*;
import static java.lang.System.exit;
import java.util.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
 
// Server class

/* Some part of the code was created with the help of geeksforgeeks.org */
public class Server 
{
 
     /* Vector is created for storing threads. Map is used to store groups */
    static Vector<ClientHandler> vector = new Vector<>();
    static Map <String, List<ClientHandler>> groupdb = new HashMap<String, List<ClientHandler>>();
    /* i is counter for clients. GROUP_COUNT is number of groups. Can be changed if necessary */
    static int i = 0;
    static int GROUP_COUNT = 5;
    
    
    
    public static void main(String[] args) throws IOException 
    {
        // Creating new server socket
        ServerSocket ss = new ServerSocket(1234);
         
        Socket s;
        String curr = "";
        
        /* Creating new groups. Each group represents a List of clients */
        for(int j = 0; j < GROUP_COUNT; j++){
            curr = "Group" + (j+1);
            List<ClientHandler> list = new ArrayList<ClientHandler>();
            groupdb.put(curr, list);
        }
        
        
        /* Infinite loop for getting client requests*/
        while (true) 
        {
            // Accept the incoming request
            s = ss.accept();
 
            System.out.println("New client request received : " + s);
             
            // obtain input and output streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                       
 
            // Create a new handler object for handling this request.
            ClientHandler mtch = new ClientHandler(s, dis, dos);
 
            // Create a new Thread with this object.
            Thread t = new Thread(mtch);
             
 
            // add this client to active clients list
            vector.add(mtch);
 
            // start the thread.
            t.start();
            
            // increment i for new client.
            // i is used for naming only, and can be replaced
            // by any naming scheme
            i++;
         
          }
    }
}
 
// ClientHandler class
class ClientHandler implements Runnable 
{
    Scanner scn = new Scanner(System.in);   // Reading from keyboard
    private String name;                    // Client name
    final DataInputStream dis;               
    final DataOutputStream dos;
    Socket s;
    boolean isloggedin;                     // Checking if user is logged in
    static Set<String> namedb = new HashSet<String>();  // Database of user names. Used to avoid same usernames
    private String groupname;                           // Name of group that user joined 
    boolean isJoined;                 // Check if used joined any group
    boolean connLost = false;          // Check if connection is lost
  
    // constructor
    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.s = s;
        this.isloggedin=true;
        this.isJoined = false;
        this.groupname = null;
       
    }
    
    /* Setter for name */
    public void SetName(String n){
        this.name = n;
    }
    
     /* Getter for name */
    public String GetName(){
        return this.name;
    }
    
     /* Getter for group name */
    public String getGroupName(){
        return groupname;
    }
    
    /* Printing all groups with user names */
    public void ListGroups() throws IOException{
        Iterator it = Server.groupdb.entrySet().iterator();
        
        
        while (it.hasNext()) {
            String str = "";
            String cl = ""; 
            
            Map.Entry pair = (Map.Entry)it.next();
            String name = (String) pair.getKey();
            List<ClientHandler> temp = (List<ClientHandler>) pair.getValue();
            
            
            for (ClientHandler mc : temp){
                if(mc.isloggedin){
                    cl += mc.GetName()+ " ";
                }
            }
            str = name + ": " + cl;
            System.out.println(str);
            
        }
    }
    
    /* Joining the given group */
    public void JoinGroup(String name) {
        Server.groupdb.get(name).add(this);
    }
    
    /* Exiting the given group */
    public void leaveGroup(String name) {
        Server.groupdb.get(name).remove(this);
    }
    
    /* Listing members of the current group */
    public void listMembers(String s) throws IOException{
        List<ClientHandler> temp = Server.groupdb.get(s);
        for (ClientHandler mc : temp){
                dos.writeUTF(mc.GetName() + " ");
            }
    }
    
    /* Sending message to all clients in current group */
    public void sendToAll(String msg) throws IOException {
        for (ClientHandler cli : Server.vector){
            if(cli.getGroupName().equals(this.groupname)) {
                if(cli.isloggedin) {
                    cli.dos.writeUTF(this.name + " : " + msg);
                }
            }
        }
    }
    @Override
    public void run(){
        
        String received; 
        String line;
        String str;
        
        /* adding 'toall' as a name in order to send message to all clients */
        if(!namedb.contains("toall")){
            namedb.add("toall");
        }
        
        /* Greeting words */
        try {
            dos.writeUTF("Hello new user!\nIn order to join the chat, type the following keywords:");
            dos.writeUTF("server hello \"your_username\"");
            dos.writeUTF("------------------------------------------------------------------------");
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /* This loop is used to initialize username and check it for validty */
        while(true){
            try {
                
                line = dis.readUTF();
                StringTokenizer defaultTokenizer = new StringTokenizer(line);
                 String temp[] = new String[3];
                int count = defaultTokenizer.countTokens();
                int j = 0;
                while (defaultTokenizer.hasMoreTokens()) {
                        temp[j] = defaultTokenizer.nextToken();
                        j++;
                }
                if(count == 3 && temp[1].equals("hello") && temp[0].equals("server")) {
                
                if(namedb.contains(temp[2])){
                    dos.writeUTF("This username already exists!");
                    continue;
                }
                namedb.add(temp[2]);
                dos.writeUTF("Hello " + temp[2]);
                SetName(temp[2]);
                System.out.println("Username assigned: " + this.name);
                break;
                
               } else {
                    dos.writeUTF("Server says: I cannot understand you! Please try again\n");
                    continue;
                }
            
            
            } catch (IOException ex) {
                System.out.println("Connection has lost");
                this.isloggedin = false;
                 this.connLost = true;
                 break;
                 //Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
        
        /* Main loop to receive commands */
        while (true){
            
            try {
                
                /* Reading received query */
                received = dis.readUTF();   
                //System.out.println(received);
                str = received;
                
                /* Getting the first word of query */
                StringTokenizer tok = new StringTokenizer(str, " ");
                String recipient = tok.nextToken();
                
                /* Checking if first word is username 
                If it is, sending other part of query as a message */
            if(namedb.contains(recipient)) {    
                
                
                
                String MsgToSend = "";
                while(tok.hasMoreTokens()){
                        MsgToSend += tok.nextToken() + " ";
                }
                
                if(recipient.equals("toall")) {
                        sendToAll(MsgToSend);
                    }
               
                
                for (ClientHandler mc : Server.vector)
                {
                    // if the recipient is found, write on its
                    // output stream
                    if(!isJoined) {
                        dos.writeUTF("You should join group first");
                        continue;
                    }
                    
                    if (mc.name.equals(recipient) && mc.isloggedin == true && !mc.name.equals("toall"))
                    {
                       if(mc.getGroupName().equals(groupname)) { 
                           mc.dos.writeUTF(this.name+" : "+MsgToSend);
                           break;
                       } else {
                           dos.writeUTF("This receiver is not in your group");
                           break;
                       }   
                    } 
                   
                 
                }
                
               /* Else if first word is not username, checking the query for a specific command */ 
            } else if(!namedb.contains(recipient)) {
                
                /* if "logout", exiting the loop, finishing execution of current thread,
                then closing the connection */
                if(received.equals("logout")){
                    this.isloggedin = false;
                    this.leaveGroup(this.groupname);
                    namedb.remove(this.name);
                    break;
                
                /* if "server grouplist", printing all the groups with members */    
                } else if(received.equals("server grouplist")) {
                        dos.writeUTF("------------------------------------------------------------------------");
                        ListGroups();
                        dos.writeUTF("------------------------------------------------------------------------");
                        continue;
                /* if "server join ... ", getting the third word of query as groupname, and entering the group
                   Otherwise, printing error message */         
                } else if(received.contains("server") && received.contains("join")) {
                    
                        if(isJoined == true){
                            dos.writeUTF("You are already joined " + this.getGroupName());
                            continue;
                        }
                            
                        StringTokenizer st = new StringTokenizer(received, " ");
                        int count = st.countTokens();
                        if(count != 3){
                            dos.writeUTF("No such group exist\nPlease try again\n");
                            continue;
                        }
                        String s1 = null;
                        while(st.hasMoreTokens()) {
                            s1 = st.nextToken();
                        }
                        if(!Server.groupdb.containsKey(s1)) {
                            dos.writeUTF("No such group exist\nPlease try again\n");
                            continue;
                        }
                        JoinGroup(s1);
                        this.groupname = s1;
                        this.isJoined = true;
                        dos.writeUTF("Welcome to the group \"" + this.groupname + "\"!\n");
                        System.out.println("User " + this.GetName() + " joined group \"" + this.groupname + "\"" );
                        continue;
                        
                    /* if "server leave ... ", getting the third word of query as groupname, and entering the group
                       Otherwise, printing error message */     
                } else if(received.contains("server") && received.contains("leave")){
                        
                        if(isJoined == false){
                            dos.writeUTF("You don't consist in any group!!!\n");
                            continue;
                        }
                            
                        StringTokenizer st = new StringTokenizer(received, " ");
                        int count = st.countTokens();
                        if(count != 3){
                            dos.writeUTF("Server cannot understand your query :( \nPlease try again\n");
                            continue;
                        }
                        String s1 = null;
                        while(st.hasMoreTokens()) {
                            s1 = st.nextToken();
                        }
                        if(!Server.groupdb.containsKey(s1)) {
                            dos.writeUTF("You mentioned wrong group name!\nPlease try again\n");
                            continue;
                        }
                        leaveGroup(s1);
                        dos.writeUTF("You exited group \"" + this.groupname + "\"\n Goodbye!");
                        System.out.println("User " + this.GetName() + " exited group \"" + this.groupname + "\"" );
                        this.groupname = null;
                        this.isJoined = false;
                       
                        continue;
                
                /* If "server members", prinring the members of current group */
                } else if(received.equals("server members")){
                        if(isJoined == true){
                            dos.writeUTF("------------------------------------------------------------------------");
                            dos.writeUTF("Members of " + groupname + ":");
                            listMembers(groupname);
                            dos.writeUTF("------------------------------------------------------------------------");
                            continue;
                        } else {
                            dos.writeUTF("You should join the group first\n");
                            continue;
                        }
                 /* If any other query, server will give error message */        
                } else {
                    dos.writeUTF("Server says: I cannot understand you!\n");
                    dos.writeUTF("Please, check the correctness of your input");
                    continue;
                }
            }
            
        } catch (IOException e) {
                
              //  e.printStackTrace();
              if(this.connLost == false)  {
                System.out.println("Connection of user " + this.name + " has lost");
                this.leaveGroup(this.groupname);
                namedb.remove(this.name);
                this.isloggedin = false;
                this.connLost = true;
              }
                break;
            }
            
        }
        try {
            
            if(this.connLost == false) {
            /* closing resources */
            dos.writeUTF("You successfully logged out");
            System.out.println("User " + this.GetName() + " logged out");
            Thread.currentThread().interrupt();
            this.dis.close();
            this.dos.close();
          
            this.s.close();
            }  
            
        }catch(IOException e){
            e.printStackTrace();
        } 
            
        
    
   }
}