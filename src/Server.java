import java.net.ServerSocket;
import java.net.Socket;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.*;
import java.io.*;

public class Server extends JFrame{
	//服务器界面
	GraphicsEnvironment ge = 
			GraphicsEnvironment.getLocalGraphicsEnvironment();
	Rectangle rec = 
			ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
	
	int width = (int)rec.getWidth();
	int height = (int)rec.getHeight();
	int w = 400;
	int h = 200;	
	
	/*————————————————————————————————————————————————————————————*/
	private ServerSocket ss=null;
	private Socket s = null;
	//不能在构造函数内定义，必须在此处定义并置空
	private DataOutputStream dos=null;
	private DataInputStream dis=null;
	private FileOutputStream fos=null;
	private FileInputStream fis=null;   
	private String path=null;//可以代表用户文件夹子目录，动态变化
	private String rootpath=null;//指向当前用户文件夹根目录

	public Server(int port) {
		this.setTitle("这是余越开发的网盘服务器端");
		this.setSize(w, h);
		this.setLocation((width-w)/2,(height-h)/2);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setVisible(true);

		try {
			ss=new ServerSocket(port);
			s=ss.accept();
			//接收套接字输出流
			dos=new DataOutputStream((s.getOutputStream()));
			//接收套接字输入流
			dis=new DataInputStream(s.getInputStream());
		} catch (IOException e) {}
		
		Thread th=new Thread(new Runnable() {
			public void run() {
				while(true) {
					NetDiskOperate(inStream());//
				}
			}
		});
		
		th.start();//线程启动
	}
	
	public void NetDiskOperate(String hint) {
		if(hint!=null) {
			String[] hint_str=hint.split("#");//
			switch(hint_str[0]) {
			case "wantCreateClientDisk":
				createFolder("G:\\NetDisk"+File.separatorChar+hint_str[1]);//以用户名建立文件夹
				//文件路径
				path=rootpath="G:\\NetDisk"+File.separatorChar+hint_str[1];
				File f_temp=new File(path);
				intoClientDisk(f_temp);
				break;
				
			case "wantCreateNewFolder":
				if(hint_str.length==2) {
					//先执行再判断
					if(!createFolder(path+File.separatorChar+hint_str[1])) {
						outStream("error#");
					}	
					else{
					}
				}
				break;
				
			case "wantOpen":
				if(hint_str[1]!=null) {
					File file_open=new File(path+File.separatorChar+hint_str[1]);
					if(file_open.isDirectory()) {
						path=path+File.separatorChar+hint_str[1];
						intoClientDisk(file_open);
					}else {
					}
				}
				break;
				
			case "wantDownload":
				File f_down=new File(path+File.separatorChar+hint_str[1]);
				if(f_down.exists()) {
					if(f_down.isFile()) {
						outStream("startDownload#");
						try {
							sendFileStream(f_down);
						} catch (Exception e) {}
					}
				}
				break;
			
			case "wantUpload":
				if(new File(path+File.separatorChar+hint_str[1]).exists()) {
				}else {
					outStream("startUpload#");
					try {
						loadFileStream(path);
					} catch (Exception e) {}
					outStream("startFlush#");
				}
				break;
				
			case "wantDelete":
				File file_delete=new File(path+File.separatorChar+hint_str[1]);
				if(file_delete.exists()) {//
					file_delete.delete();
				}
				outStream("wantFlush#");
				break;
				
			case "wantRename":
				File file_rename=new File(path+File.separatorChar+hint_str[1]);
				if(file_rename.isDirectory()) {
					if(!file_rename.renameTo(new File(path+File.separatorChar+hint_str[2]))) {
						outStream("error#");
					}
					else{
						break;
					}
				}
				else {//
					String prefix=hint_str[1].substring(hint_str[1].lastIndexOf("."));
					if(!file_rename.renameTo(new File(path+File.separatorChar+hint_str[2]+prefix))) {
						outStream("error#");
					}
					else{
						break;
					}
				}
				break;
				
			case "wantFlush":
				File fileFlush = new File(path);
				intoClientDisk(fileFlush);
				break;
				
			case "wantBack":
				File back = new File(path);
				if(!back.getAbsolutePath().toString().equals(rootpath)) {//当前目录不为空		
					back = back.getParentFile();
					path = back.getAbsolutePath();
				}
				intoClientDisk(back);
				break;
			
			/*default:
				System.out.println("客户端传输指令有误，请检查!");*/
			}
		}	
		
	}

	public void intoClientDisk(File f) {
		//返回一个抽象路径名数组，表示目录中的文件
		File[] fileList=f.listFiles();
		StringBuffer file=new StringBuffer("返回上级目录");
		for(int i=0;i<fileList.length;i++) {
			if(fileList[i].isDirectory()) {
				file.append("#"+fileList[i].getName());
			}
			if(fileList[i].isFile()) {
				//返回由此抽象路径名表示的文件或目录的名称
				file.append("#"+fileList[i].getName());
			}
		}
		outStream(file.toString());
	}
	
	//创建目录
	public boolean createFolder(String path) {
        File directory = new File(path);
        if(!directory.exists()) {  
        	//此目录不存在,创建此目录
            directory.mkdir();
            return true;
        } 
        else {
        	return false;
        }
	}
	
	public void outStream(String ostr) {
		try {
			dos.writeUTF(ostr);
			dos.flush();
		} catch (IOException e) {}
	}
	
	public String inStream() {
		String oStream = null;
		try {
			//阻塞式
			oStream = dis.readUTF();
		} catch (IOException e) {}
		return oStream;
	}
	
	//服务器发送字节流给客户端函数
	public void sendFileStream(File f) throws Exception{
		try {
            byte[] fileByte = new byte[1024]; 
			System.out.println("客户端开始从服务器下载 "+f.getName()+" ...");
			fis=new FileInputStream(f);
			dos.writeUTF(f.getName());
			dos.flush();
			long len=f.length();
			dos.writeLong(len);
			dos.flush(); 
            int length = 0;  
            while(len>0) { 
            	length = fis.read(fileByte, 0, 1024);
                dos.write(fileByte, 0, length);  
                dos.flush();  
            }  
			System.out.println("文件下载完成，下载文件在网盘中原位置为："+f.getAbsolutePath());
		}catch(Exception e) {}finally {
			outStream("finishDownload#"+f.getName());
			fis.close();
		}
	}
	
	//服务器接受客户端上传字节流函数
	public void loadFileStream(String path) throws IOException {
		String loadFileName = dis.readUTF();  
		try {
			byte[] fileByte = new byte[1024]; 
			long fileLength = dis.readLong();
			File f_temp = new File(path + File.separatorChar + loadFileName); 
			System.out.println("客户端正在上传 "+ f_temp.getName()+"到服务器...");
			fos = new FileOutputStream(f_temp); 
			int length = 0;   
			while(fileLength>0) { 
				length=dis.read(fileByte, 0, 1024);
				fos.write(fileByte, 0, length);  
				fos.flush();  
				fileLength -= 1024;
			} 
			System.out.println(f_temp.getName()+"上传成功，在网盘中地址为"+f_temp.getAbsolutePath());
		}catch(Exception e) {}finally {
			outStream("finishUpload#"+loadFileName);
			fos.close();
		}
	}
	public static void main(String[] args){

		new Server(9999);
	}
}