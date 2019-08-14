/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server2;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
 

/* Some part of the code was created with the help of geeksforgeeks.org */

public class Client 
{
    final static int ServerPort = 1234;
    
    public static void main(String args[]) throws UnknownHostException, IOException 
    {
        Scanner scn = new Scanner(System.in);
          
        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");
         
        // establish the connection
        Socket s = new Socket(ip, ServerPort);
         
        // obtaining input and out streams
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        
        Thread sendMessage = new Thread(new Runnable() 
        {
            @Override
            public void run() {
               
                
                while (true) {
                    
                    // read the message to deliver.
                    String msg = scn.nextLine();
                    
                    try {
                        // write on the output stream
                        //System.out.println("Write Message to deliver");
                        dos.writeUTF(msg);
                    } catch (IOException e) {
                    }
                }
            }
        });
         
        // readMessage thread
        Thread readMessage = new Thread(new Runnable() 
        {
            @Override
            public void run() {
 
                while (true) {
                    try {
                        // read the message sent to this client
                        String msg = dis.readUTF();
                        
                        System.out.println(msg);
                    } catch (IOException e) {
                    }
                }
                
            }
        });
        
        
        
        sendMessage.start();
        readMessage.start();
        
        
    }
}
