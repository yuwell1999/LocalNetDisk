import java.net.ServerSocket;
import java.net.Socket;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.*;
import java.io.*;

public class Server extends JFrame{
	//����������
	GraphicsEnvironment ge = 
			GraphicsEnvironment.getLocalGraphicsEnvironment();
	Rectangle rec = 
			ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
	
	int width = (int)rec.getWidth();
	int height = (int)rec.getHeight();
	int w = 400;
	int h = 200;	
	
	/*������������������������������������������������������������������������������������������������������������������������*/
	private ServerSocket ss=null;
	private Socket s = null;
	//�����ڹ��캯���ڶ��壬�����ڴ˴����岢�ÿ�
	private DataOutputStream dos=null;
	private DataInputStream dis=null;
	private FileOutputStream fos=null;
	private FileInputStream fis=null;   
	private String path=null;//���Դ����û��ļ�����Ŀ¼����̬�仯
	private String rootpath=null;//ָ��ǰ�û��ļ��и�Ŀ¼

	public Server(int port) {
		this.setTitle("������Խ���������̷�������");
		this.setSize(w, h);
		this.setLocation((width-w)/2,(height-h)/2);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setVisible(true);

		try {
			ss=new ServerSocket(port);
			s=ss.accept();
			//�����׽��������
			dos=new DataOutputStream((s.getOutputStream()));
			//�����׽���������
			dis=new DataInputStream(s.getInputStream());
		} catch (IOException e) {}
		
		Thread th=new Thread(new Runnable() {
			public void run() {
				while(true) {
					NetDiskOperate(inStream());//
				}
			}
		});
		
		th.start();//�߳�����
	}
	
	public void NetDiskOperate(String hint) {
		if(hint!=null) {
			String[] hint_str=hint.split("#");//
			switch(hint_str[0]) {
			case "wantCreateClientDisk":
				createFolder("G:\\NetDisk"+File.separatorChar+hint_str[1]);//���û��������ļ���
				//�ļ�·��
				path=rootpath="G:\\NetDisk"+File.separatorChar+hint_str[1];
				File f_temp=new File(path);
				intoClientDisk(f_temp);
				break;
				
			case "wantCreateNewFolder":
				if(hint_str.length==2) {
					//��ִ�����ж�
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
				if(!back.getAbsolutePath().toString().equals(rootpath)) {//��ǰĿ¼��Ϊ��		
					back = back.getParentFile();
					path = back.getAbsolutePath();
				}
				intoClientDisk(back);
				break;
			
			/*default:
				System.out.println("�ͻ��˴���ָ����������!");*/
			}
		}	
		
	}

	public void intoClientDisk(File f) {
		//����һ������·�������飬��ʾĿ¼�е��ļ�
		File[] fileList=f.listFiles();
		StringBuffer file=new StringBuffer("�����ϼ�Ŀ¼");
		for(int i=0;i<fileList.length;i++) {
			if(fileList[i].isDirectory()) {
				file.append("#"+fileList[i].getName());
			}
			if(fileList[i].isFile()) {
				//�����ɴ˳���·������ʾ���ļ���Ŀ¼������
				file.append("#"+fileList[i].getName());
			}
		}
		outStream(file.toString());
	}
	
	//����Ŀ¼
	public boolean createFolder(String path) {
        File directory = new File(path);
        if(!directory.exists()) {  
        	//��Ŀ¼������,������Ŀ¼
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
			//����ʽ
			oStream = dis.readUTF();
		} catch (IOException e) {}
		return oStream;
	}
	
	//�����������ֽ������ͻ��˺���
	public void sendFileStream(File f) throws Exception{
		try {
            byte[] fileByte = new byte[1024]; 
			System.out.println("�ͻ��˿�ʼ�ӷ��������� "+f.getName()+" ...");
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
			System.out.println("�ļ�������ɣ������ļ���������ԭλ��Ϊ��"+f.getAbsolutePath());
		}catch(Exception e) {}finally {
			outStream("finishDownload#"+f.getName());
			fis.close();
		}
	}
	
	//���������ܿͻ����ϴ��ֽ�������
	public void loadFileStream(String path) throws IOException {
		String loadFileName = dis.readUTF();  
		try {
			byte[] fileByte = new byte[1024]; 
			long fileLength = dis.readLong();
			File f_temp = new File(path + File.separatorChar + loadFileName); 
			System.out.println("�ͻ��������ϴ� "+ f_temp.getName()+"��������...");
			fos = new FileOutputStream(f_temp); 
			int length = 0;   
			while(fileLength>0) { 
				length=dis.read(fileByte, 0, 1024);
				fos.write(fileByte, 0, length);  
				fos.flush();  
				fileLength -= 1024;
			} 
			System.out.println(f_temp.getName()+"�ϴ��ɹ����������е�ַΪ"+f_temp.getAbsolutePath());
		}catch(Exception e) {}finally {
			outStream("finishUpload#"+loadFileName);
			fos.close();
		}
	}
	public static void main(String[] args){

		new Server(9999);
	}
}