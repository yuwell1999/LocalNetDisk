import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;


class LoginPage extends JFrame{
	//���ô����С
	GraphicsEnvironment ge = 
			GraphicsEnvironment.getLocalGraphicsEnvironment();
	Rectangle rec = 
			ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
	
	int width = (int)rec.getWidth();
	int height = (int)rec.getHeight();
	static int w = 400;
	static int h = 150;
	
	private JPanel jp_base = new JPanel();
	private JPanel jp_label = new JPanel();
	private JPanel jp_type = new JPanel();
	
	private String ipAddress = null;
	private int port = 0;
	private Client user = null;
	private NetDiskFrame nd = null;

	//�û����������ı���
	private JTextField jtf = new JTextField(10);
	private JButton bt_Login = new JButton("��¼");
	private String msg = null;	
	private boolean next = false;
	
	//�������
	public LoginPage(NetDiskFrame nd,Client user,String ipAddress,int port){
		//���캯��
		this.nd = nd;
		this.user = user;
		this.ipAddress = ipAddress;
		this.port = port;
		
		jp_base.setLayout(new BorderLayout(10,10));
		jp_label.add(new JLabel("�����ǳ�:"));
		jp_type.add(jtf);

		jp_base.add(jp_label, BorderLayout.NORTH);
		jp_base.add(jp_type, BorderLayout.CENTER);
		jp_base.add(bt_Login, BorderLayout.SOUTH);
		
		//JFrame���
		this.add(jp_base);
		this.setTitle("��ӭʹ����Խ���������̿ͻ���V1.0");
		this.setSize(w, h);
		this.setLocation(width/2-w/2, height/2-h/2);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		//��½��ť��
		bt_Login.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent e){	
				createConnection(user,ipAddress,port);	
				System.out.println("���ӳɹ�");
			}
		});
	}
	
	public void createConnection(Client user,String ipAddress,int port) {
		//�û���û�����뱨��
		if(jtf.getText().length() == 0) {
				JOptionPane.showMessageDialog(this,"�������û���","����",JOptionPane.WARNING_MESSAGE);
		}else{
			user = new Client(ipAddress,port);//Ϊ��ǰ�û�����������
			if(!user.clientState()){//������δ��
				JOptionPane.showMessageDialog(this,"����ʧ�ܣ���������","������ʾ",JOptionPane.WARNING_MESSAGE);
				this.dispose();//�ͷ���Դ
			}else {
				this.setVisible(false);
				//��������
				nd = new NetDiskFrame(user,jtf.getText());
				nd.setVisible(true);
				this.dispose();
			}
		}
	}
}

class NetDiskFrame extends JFrame{
	//�������û�������һ��ͬ����Ҫ���ô����С
	private Client user = null;
	
	GraphicsEnvironment ge = 
			GraphicsEnvironment.getLocalGraphicsEnvironment();
	Rectangle rec = 
			ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
	
	int width = (int)rec.getWidth();
	int height = (int)rec.getHeight();
	static int w = 400;
	static int h = 300;
	
	String path = null;
	JPanel jp = new JPanel();
	JPanel jp1 = new JPanel();
	
	//����ͻ������
	private JMenuBar jbar = new JMenuBar();
	private JPopupMenu popupMenu = new JPopupMenu();
	private JMenu fileMenu = new JMenu("���̲���");
	
	//���ò˵��е���
	private JMenuItem mit_new = new JMenuItem("�½��ļ���");
	private JMenuItem mit_return = new JMenuItem("�����ϼ�");
	private JMenuItem mit_delete = new JMenuItem("ɾ��");
	private JMenuItem mit_rename = new JMenuItem("������");
	private JMenuItem mit_download = new JMenuItem("����");
	private JMenuItem mit_upload = new JMenuItem("�ϴ�");
	private JFileChooser jfc = new JFileChooser();
	//�ļ��б�
	private JList fdl = new JList();
	
	//NetDiskFrame���캯��
	public NetDiskFrame(Client user,String userName) {
		
		this.user = user;
		//�����½����û���Ϊ�ļ����Ĳ���
		user.outStream("wantCreateClientDisk#"+userName);
		this.setLayout(new BorderLayout());
		this.setTitle("�����ڲ���" + userName + "������");
		
		//�����С
		this.setSize(w,h);
		this.setLocation((width-w)/2, (height-h)/2);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		jp.setLayout(new BorderLayout());
		jp.add(jp1,BorderLayout.EAST);
		this.add(jp, BorderLayout.EAST);
		
		//�ļ��б����
		fdl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);//��ѡģʽ
		fdl.setFixedCellHeight(20);//�б�Ԫ�߶�
		fdl.setFixedCellWidth(300);//�б�Ԫ�߶�
		//���ù�����������Ҫʱ��ʾ
		JScrollPane js = new JScrollPane(fdl,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.add(js,BorderLayout.CENTER);
		
		//����˵��Ͱ�ť
		fileMenu.add(mit_new);
		fileMenu.add(mit_download);
		fileMenu.add(mit_upload);
		fileMenu.add(mit_return);
		jbar.add(fileMenu);
        //�Ҽ��˵�
		popupMenu.add(mit_delete);
		popupMenu.add(mit_rename);
		this.setJMenuBar(jbar);
	
		Thread th = new Thread(new Runnable() {
			public void run() {
				while(true) {
					NetDiskOperate(user.inStream());
				}
			}
		});
		
		th.start();
		
		/*----------------��ز�ͬ��ť����--------------------*/
		
		//������
		fdl.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == e.BUTTON3) {
					popsetLocation(e.getX(),e.getY());
				}
				if(e.getClickCount() == 2) {
					if(fdl.getSelectedIndex() == 0) {
						user.outStream("wantBack#");
					}
					else{
						user.outStream("wantOpen#"+fdl.getSelectedValue());
					}
				}
			}
		});
		
		//�Ҽ��ϴ��ļ���ť
		mit_upload.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent event){
				path = uploadFile();
				if(path == null) {
					showErrorMessage("����","��ѡ��Ҫ�ϴ����ļ�");
				}else{
					user.outStream("wantUpload#"+new File(path).getName());
				}
			}
		});
		
		//�Ҽ������ļ���ť
		mit_download.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent event){
				if(fdl.getSelectedValue() == null) {
					showErrorMessage("������ʾ","��ѡ��Ҫ���ص��ļ�");
				}else{
					path = downloadFile();
					if(path == null) {}
					else{
						File f_download = new File(path+File.separatorChar+fdl.getSelectedValue());
						if(f_download.exists()) {
							showErrorMessage("������ʾ","���ļ��Ѿ�����");
						}else{
							user.outStream("wantDownload#"+fdl.getSelectedValue());
						}
					}
				}
			}
		});

		//�½��ļ��а�ť����
		mit_new.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent e){
				String newFolderName = inputFolderName("ϵͳ��ʾ");
				if(newFolderName == null) {}
				else if(newFolderName.length() == 0) {
					showErrorMessage("����","�������������ļ�����");
				}else{
					user.outStream("wantCreateNewFolder#"+newFolderName);
					user.outStream("wantFlush#");
				}
			}
		});
		
		//�����ϼ��˵���ť����
		mit_return.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent e){
				user.outStream("wantBack#");
			}
		});
		
		//�Ҽ�ɾ���ļ�����
		mit_delete.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent e){
				if(fdl.getSelectedValue() == null) {//ѡ��Ҫɾ���ļ�
					showErrorMessage("����","��ѡ��ɾ�����ļ�����ļ���");
				}else if(deleteCheck()) {//��ȷ�Ͽ��е��ȷ��
					user.outStream("wantDelete#"+fdl.getSelectedValue());
					user.outStream("wantFlush#");
				}
			}
		});
		
		//�Ҽ�����������
		mit_rename.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent e){
				if(fdl.getSelectedValue() == null) {
					showErrorMessage("����","����ȷѡ���������ļ�");
				}else {
					String name = inputFolderName("������");
					if(name == null) {}
					else if(name.length() == 0) {//�ļ�������Ϊ��
						showErrorMessage("������ʾ","���������ļ���");
					}else {
						user.outStream("wantRename#"+fdl.getSelectedValue()+"#"+name);
						user.outStream("wantFlush#");
					}			
				}
			}
		});
		
	}
	public void NetDiskOperate(String hint) {//���ݴ����hintָ����в���
		if(hint!=null) {
			String[] hint_str = hint.split("#");//�Ѵ�������hintָ�����#���зָ�
			switch(hint_str[0]) {
			case "startDownload"://��ʼ����
				try {
					user.loadFileStream(path);
				} catch (IOException e) {}
				break;
				
			case "startUpload"://��ʼ�ϴ�
				try {
					user.sendFileStream(path);//�����ļ��ֽ����غ���
				} catch (Exception e) {}
				break;
				
			case "finishDownload":
				showPlainMessage("�������̵���ʾ","�������");
				break;
				
			case "finishUpload":
				showPlainMessage("�������̵���ʾ","�ϴ����");
				break;	
				
			case "�����ϼ�Ŀ¼":
				changeDirectory(hint_str);
				break;
				
			case "startFlush":
				user.outStream("wantFlush#");
				break;
				
			case "error":
				showPlainMessage("������ʾ","���ļ��Ѵ��ڻ����ļ�������ȷ");
				break;
				
			default:
				break;
			}
		}
	}
	
	//�ļ����ش�����
	public String downloadFile() {
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);//����Ŀ¼Ϊֻ��
		if(jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ) {//����һ�� "Save File" �ļ�ѡ������ѡ��ȷ�Ϻ󷵻ظ�ֵ
			return jfc.getSelectedFile().getAbsolutePath();//����ļ��ľ���·��
		}
		else {
			return null;
		}
	}
	
	//�ļ��ϴ�������
	public String uploadFile() {
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);//�ļ�ֻ��
		if(jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ) {
			return jfc.getSelectedFile().getAbsolutePath();
		}
		else {
			return null;
		}
	}
	
	//�����ļ����ļ������ֺ���
	public String inputFolderName(String folderName) {
		return JOptionPane.showInputDialog(this,"���������ļ�(��)��",folderName,JOptionPane.PLAIN_MESSAGE);
	}
	
	//��ȡ�ļ�����
	public void changeDirectory(String[] str) {
		fdl.setListData(str);
	}
	
	//���������Ϣ����
	public void showErrorMessage(String title,String msg) {
		JOptionPane.showMessageDialog(this,msg,title,JOptionPane.WARNING_MESSAGE);
	}
	
	//�����ͨ��Ϣ����
	public void showPlainMessage(String title,String msg) {
		JOptionPane.showMessageDialog(this,msg,title,JOptionPane.INFORMATION_MESSAGE);
	}
	
	//ɾ��ȷ�Ϻ���
	public boolean deleteCheck() {
		return JOptionPane.YES_OPTION == (JOptionPane.showConfirmDialog(this, "ȷ��ɾ�����ļ����ò������ɳ�����","��ȷ��",JOptionPane.YES_NO_OPTION));
	}
	
	//�Ҽ�������λ����
	public void popsetLocation(int x,int y) {
		popupMenu.show(this, x, y);
	}
}
public class Client {
	//�����ļ�������������������������
	private FileOutputStream fos = null;
	private FileInputStream fis = null;
	private DataOutputStream dos = null;
	private DataInputStream dis = null;
	
	private long len = 1;
	Socket  client = null;
	
	//�û��Ƿ��Ѵ���
	public boolean clientState() { 
		return client != null;//�û�����
	}
	
	public Client(String ipAddress,int port){
		client = null;
		try {
				client = new Socket(ipAddress,port);
				dos = new DataOutputStream((client.getOutputStream()));//�����׽��������
				dis = new DataInputStream(client.getInputStream());//�����׽��������
		} catch (IOException e) {}	
	}
	
	//
	public void outStream(String ostr) {
		try {
			dos.writeUTF(ostr);//�ַ���д����������������
			dos.flush();//����������������ʹ���л��������ֽڱ�д��������
		} catch (IOException e) {}
	}
	
	//
	public String inStream() {
		String istr = null;
		try {
			istr = dis.readUTF();//���������ж�ȡ�ֽ�
		} catch (IOException e) {}
		return istr;
	}
	
	//�ļ��ֽ��ϴ�����
	public void sendFileStream(String path) throws Exception{
		try {
			File f_temp = new File(path);//ͨ��������·�����ַ���ת��Ϊ����·����������һ���� File ʵ��
			if(f_temp.exists()) {
				fis = new FileInputStream(f_temp);
				dos.writeUTF(f_temp.getName());//�����ɴ˳���·������ʾ���ļ���Ŀ¼�����ƣ�д����������
				dos.flush();
				dos.writeLong(f_temp.length());//�ļ�����д�������
				dos.flush(); 
				//�ļ���byteΪ��λ���䣬���鳤�����⣬��Ϊ����һ������һ��
                byte[] fileByte = new byte[1024];  
                int length = 0;  
                len = f_temp.length();
                //�Ӵ��������н���� length���ֽڵ����ݶ���һ�� byte �����С���� len��Ϊ0�������������֮ǰ���÷���������
                while((length = fis.read(fileByte, 0, fileByte.length)) != -1) {//���뻺�������ֽ������������Ϊ�Ѿ������ļ�ĩβ��û�и�������ݣ��򷵻� -1  
                    dos.write(fileByte, 0, length);  
                    dos.flush();     
                }  	
                System.out.println("�ļ�" + f_temp.getName() + "�������");
			}
		}catch(Exception e) {}
		finally {
			fis.close();
		}
	}
	
	//�ļ��ֽ����غ���
	public void loadFileStream(String path) throws IOException {
		try {
			String loadFileName = dis.readUTF();//�������ȡ�ļ���
			long loadFileLength = dis.readLong();
			len = loadFileLength; 
			//System.out.println(path + File.separatorChar + loadFileName);
			byte[] fileByte = new byte[1024];  
			int length = 0;   
		
			//��ϵͳ�йص�Ĭ�����Ʒָ��������ֶα���ʼ��Ϊ����ϵͳ���� File.separator ֵ�ĵ�һ���ַ���
			//�� Microsoft Windows ϵͳ�ϣ���Ϊ '\\'���������Ϊ ����·��\\�ļ���
			
			//����·���������½�һ��fileʵ��
			//�����ļ�ʵ��Ϊ�գ���д������
			File f_temp = new File(path + File.separatorChar + loadFileName); 
			System.out.println("�ļ�" + f_temp.getName() + "��������");
			fos = new FileOutputStream(f_temp);
			while(loadFileLength>0) { 
				length = dis.read(fileByte, 0, 1024);
				fos.write(fileByte, 0, length);  
				fos.flush();  
				loadFileLength -= 1024;
			}  
		}catch(Exception e) {}
		fos.close();
	}

	public static void main(String[] args){
		Client user = null;
		NetDiskFrame nd = null;
		new LoginPage(nd,user,"127.0.0.1",9999).setVisible(true);
	}
}