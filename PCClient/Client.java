
import java.net.*;  
import java.io.*; 
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue; 


class Client{  

    public static BlockingQueue < String > inputQueue = new LinkedBlockingQueue<String>();;

    public Socket createSocket(String host, int port) {
        try {
            Socket s = new Socket(host,port);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String executeCommand(String command) {
        String output="";
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader( new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output=output+"\n"+line;
            }
            reader.close();
            return output;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String args[])throws Exception{  

        InputThread inputThread = new InputThread(inputQueue);
        inputThread.start();

        Client myClient = new Client();
        Socket s=myClient.createSocket("192.168.0.100",3333);
        DataInputStream din=new DataInputStream(s.getInputStream());  
        DataOutputStream dout=new DataOutputStream(s.getOutputStream());  

        String serverCommand="";  
        String pwd="";

        while (true) { 
            if(!inputQueue.isEmpty())
            {
                String str = null;
                try {
                    str = inputQueue.take();
                    if(str != null)
                    {
                        System.out.println(" Data to write : " + str);

                        dout.writeUTF("nazib: " + str); 
                        dout.flush();
                    } 
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
            else{
                try {
                // receive from the server 
                    if(din.available()>0){
                        serverCommand=din.readUTF();  
                        System.out.println("COMMAND:"+serverCommand);
                    }
                } catch(Exception e){
                    e.printStackTrace();
                    break;
                }
            }
        }

        dout.close();  
        s.close();  
//

/*
        while(!serverCommand.equals("stop")){  

            // read the server commands
            serverCommand=din.readUTF();  
            System.out.println("COMMAND:"+serverCommand);
            
            //issue command
/*            String output="";
            if(serverCommand != null && !serverCommand.isEmpty()) { 
                output = myClient.executeCommand(serverCommand);
                pwd = myClient.executeCommand("pwd");
            } else {
                output="";
            }
*/
/*            // flush the output to server
            dout.writeUTF("nazib: " + serverCommand);  
            dout.flush();  
        }  
*/

    }
}  



class InputThread extends Thread {
    private BlockingQueue < String > inputQueue;
    public InputThread(BlockingQueue< String > aQueue) {
        super("InputThread");
        this.inputQueue = aQueue;
    }
    public void run() {

        BufferedReader kb  = new BufferedReader(new InputStreamReader(System.in)); 
        String str = "";

            while (true) {
                try {
                    str = kb.readLine();
                    if(str != null)
                    {
                        if(!str.isEmpty())
                        {
                            inputQueue.put(str);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                

            }

    }
}
