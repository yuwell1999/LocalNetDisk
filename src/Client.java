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
	//设置窗体大小
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

	//用户名称输入文本框
	private JTextField jtf = new JTextField(10);
	private JButton bt_Login = new JButton("登录");
	private String msg = null;	
	private boolean next = false;
	
	//进入界面
	public LoginPage(NetDiskFrame nd,Client user,String ipAddress,int port){
		//构造函数
		this.nd = nd;
		this.user = user;
		this.ipAddress = ipAddress;
		this.port = port;
		
		jp_base.setLayout(new BorderLayout(10,10));
		jp_label.add(new JLabel("输入昵称:"));
		jp_type.add(jtf);

		jp_base.add(jp_label, BorderLayout.NORTH);
		jp_base.add(jp_type, BorderLayout.CENTER);
		jp_base.add(bt_Login, BorderLayout.SOUTH);
		
		//JFrame添加
		this.add(jp_base);
		this.setTitle("欢迎使用余越开发的网盘客户端V1.0");
		this.setSize(w, h);
		this.setLocation(width/2-w/2, height/2-h/2);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		//登陆按钮绑定
		bt_Login.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent e){	
				createConnection(user,ipAddress,port);	
				System.out.println("连接成功");
			}
		});
	}
	
	public void createConnection(Client user,String ipAddress,int port) {
		//用户名没有输入报错
		if(jtf.getText().length() == 0) {
				JOptionPane.showMessageDialog(this,"请输入用户名","错误",JOptionPane.WARNING_MESSAGE);
		}else{
			user = new Client(ipAddress,port);//为当前用户建立新连接
			if(!user.clientState()){//服务器未打开
				JOptionPane.showMessageDialog(this,"连接失败，请检查网络","错误提示",JOptionPane.WARNING_MESSAGE);
				this.dispose();//释放资源
			}else {
				this.setVisible(false);
				//重新输入
				nd = new NetDiskFrame(user,jtf.getText());
				nd.setVisible(true);
				this.dispose();
			}
		}
	}
}

class NetDiskFrame extends JFrame{
	//与输入用户名界面一样同样需要设置窗体大小
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
	
	//定义客户端组件
	private JMenuBar jbar = new JMenuBar();
	private JPopupMenu popupMenu = new JPopupMenu();
	private JMenu fileMenu = new JMenu("网盘操作");
	
	//设置菜单中的项
	private JMenuItem mit_new = new JMenuItem("新建文件夹");
	private JMenuItem mit_return = new JMenuItem("返回上级");
	private JMenuItem mit_delete = new JMenuItem("删除");
	private JMenuItem mit_rename = new JMenuItem("重命名");
	private JMenuItem mit_download = new JMenuItem("下载");
	private JMenuItem mit_upload = new JMenuItem("上传");
	private JFileChooser jfc = new JFileChooser();
	//文件列表
	private JList fdl = new JList();
	
	//NetDiskFrame构造函数
	public NetDiskFrame(Client user,String userName) {
		
		this.user = user;
		//发送新建以用户名为文件名的操作
		user.outStream("wantCreateClientDisk#"+userName);
		this.setLayout(new BorderLayout());
		this.setTitle("您正在操作" + userName + "的网盘");
		
		//框体大小
		this.setSize(w,h);
		this.setLocation((width-w)/2, (height-h)/2);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		jp.setLayout(new BorderLayout());
		jp.add(jp1,BorderLayout.EAST);
		this.add(jp, BorderLayout.EAST);
		
		//文件列表操作
		fdl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);//单选模式
		fdl.setFixedCellHeight(20);//列表单元高度
		fdl.setFixedCellWidth(300);//列表单元高度
		//设置滚动条，在需要时显示
		JScrollPane js = new JScrollPane(fdl,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.add(js,BorderLayout.CENTER);
		
		//组件菜单和按钮
		fileMenu.add(mit_new);
		fileMenu.add(mit_download);
		fileMenu.add(mit_upload);
		fileMenu.add(mit_return);
		jbar.add(fileMenu);
        //右键菜单
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
		
		/*----------------监控不同按钮操作--------------------*/
		
		//鼠标操作
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
		
		//右键上传文件按钮
		mit_upload.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent event){
				path = uploadFile();
				if(path == null) {
					showErrorMessage("错误","请选中要上传的文件");
				}else{
					user.outStream("wantUpload#"+new File(path).getName());
				}
			}
		});
		
		//右键下载文件按钮
		mit_download.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent event){
				if(fdl.getSelectedValue() == null) {
					showErrorMessage("错误提示","请选择要下载的文件");
				}else{
					path = downloadFile();
					if(path == null) {}
					else{
						File f_download = new File(path+File.separatorChar+fdl.getSelectedValue());
						if(f_download.exists()) {
							showErrorMessage("错误提示","该文件已经存在");
						}else{
							user.outStream("wantDownload#"+fdl.getSelectedValue());
						}
					}
				}
			}
		});

		//新建文件夹按钮操作
		mit_new.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent e){
				String newFolderName = inputFolderName("系统提示");
				if(newFolderName == null) {}
				else if(newFolderName.length() == 0) {
					showErrorMessage("错误","请重新输入新文件名！");
				}else{
					user.outStream("wantCreateNewFolder#"+newFolderName);
					user.outStream("wantFlush#");
				}
			}
		});
		
		//返回上级菜单按钮操作
		mit_return.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent e){
				user.outStream("wantBack#");
			}
		});
		
		//右键删除文件操作
		mit_delete.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent e){
				if(fdl.getSelectedValue() == null) {//选中要删除文件
					showErrorMessage("错误","请选择删除的文件或空文件夹");
				}else if(deleteCheck()) {//在确认框中点击确认
					user.outStream("wantDelete#"+fdl.getSelectedValue());
					user.outStream("wantFlush#");
				}
			}
		});
		
		//右键重命名操作
		mit_rename.addActionListener(new ActionListener() {
			public synchronized void actionPerformed(ActionEvent e){
				if(fdl.getSelectedValue() == null) {
					showErrorMessage("错误","请正确选择重命名文件");
				}else {
					String name = inputFolderName("重命名");
					if(name == null) {}
					else if(name.length() == 0) {//文件名不能为空
						showErrorMessage("错误提示","请输入新文件名");
					}else {
						user.outStream("wantRename#"+fdl.getSelectedValue()+"#"+name);
						user.outStream("wantFlush#");
					}			
				}
			}
		});
		
	}
	public void NetDiskOperate(String hint) {//根据传入的hint指令进行操作
		if(hint!=null) {
			String[] hint_str = hint.split("#");//把传过来的hint指令根据#进行分割
			switch(hint_str[0]) {
			case "startDownload"://开始下载
				try {
					user.loadFileStream(path);
				} catch (IOException e) {}
				break;
				
			case "startUpload"://开始上传
				try {
					user.sendFileStream(path);//调用文件字节下载函数
				} catch (Exception e) {}
				break;
				
			case "finishDownload":
				showPlainMessage("来自网盘的提示","下载完成");
				break;
				
			case "finishUpload":
				showPlainMessage("来自网盘的提示","上传完成");
				break;	
				
			case "返回上级目录":
				changeDirectory(hint_str);
				break;
				
			case "startFlush":
				user.outStream("wantFlush#");
				break;
				
			case "error":
				showPlainMessage("错误提示","该文件已存在或新文件名不正确");
				break;
				
			default:
				break;
			}
		}
	}
	
	//文件下载处理函数
	public String downloadFile() {
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);//设置目录为只读
		if(jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ) {//弹出一个 "Save File" 文件选择器框，选择确认后返回该值
			return jfc.getSelectedFile().getAbsolutePath();//获得文件的绝对路径
		}
		else {
			return null;
		}
	}
	
	//文件上传处理函数
	public String uploadFile() {
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);//文件只读
		if(jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ) {
			return jfc.getSelectedFile().getAbsolutePath();
		}
		else {
			return null;
		}
	}
	
	//输入文件、文件夹名字函数
	public String inputFolderName(String folderName) {
		return JOptionPane.showInputDialog(this,"请输入新文件(夹)名",folderName,JOptionPane.PLAIN_MESSAGE);
	}
	
	//获取文件数组
	public void changeDirectory(String[] str) {
		fdl.setListData(str);
	}
	
	//输出错误信息函数
	public void showErrorMessage(String title,String msg) {
		JOptionPane.showMessageDialog(this,msg,title,JOptionPane.WARNING_MESSAGE);
	}
	
	//输出普通信息函数
	public void showPlainMessage(String title,String msg) {
		JOptionPane.showMessageDialog(this,msg,title,JOptionPane.INFORMATION_MESSAGE);
	}
	
	//删除确认函数
	public boolean deleteCheck() {
		return JOptionPane.YES_OPTION == (JOptionPane.showConfirmDialog(this, "确认删除该文件？该操作不可撤销！","请确认",JOptionPane.YES_NO_OPTION));
	}
	
	//右键弹出定位函数
	public void popsetLocation(int x,int y) {
		popupMenu.show(this, x, y);
	}
}
public class Client {
	//定义文件输入输出流、数据输入输出流
	private FileOutputStream fos = null;
	private FileInputStream fis = null;
	private DataOutputStream dos = null;
	private DataInputStream dis = null;
	
	private long len = 1;
	Socket  client = null;
	
	//用户是否已存在
	public boolean clientState() { 
		return client != null;//用户存在
	}
	
	public Client(String ipAddress,int port){
		client = null;
		try {
				client = new Socket(ipAddress,port);
				dos = new DataOutputStream((client.getOutputStream()));//接受套接字输出流
				dis = new DataInputStream(client.getInputStream());//接受套接字输出流
		} catch (IOException e) {}	
	}
	
	//
	public void outStream(String ostr) {
		try {
			dos.writeUTF(ostr);//字符串写入基础输出流，传给
			dos.flush();//清空数据输出流，迫使所有缓冲的输出字节被写出到流中
		} catch (IOException e) {}
	}
	
	//
	public String inStream() {
		String istr = null;
		try {
			istr = dis.readUTF();//从输入流中读取字节
		} catch (IOException e) {}
		return istr;
	}
	
	//文件字节上传函数
	public void sendFileStream(String path) throws Exception{
		try {
			File f_temp = new File(path);//通过将给定路径名字符串转换为抽象路径名来创建一个新 File 实例
			if(f_temp.exists()) {
				fis = new FileInputStream(f_temp);
				dos.writeUTF(f_temp.getName());//返回由此抽象路径名表示的文件或目录的名称，写入基础输出流
				dos.flush();
				dos.writeLong(f_temp.length());//文件长度写入输出流
				dos.flush(); 
				//文件以byte为单位传输，数组长度任意，因为传完一个接着一个
                byte[] fileByte = new byte[1024];  
                int length = 0;  
                len = f_temp.length();
                //从此输入流中将最多 length个字节的数据读入一个 byte 数组中。如果 len不为0，则在输入可用之前，该方法将阻塞
                while((length = fis.read(fileByte, 0, fileByte.length)) != -1) {//读入缓冲区的字节总数，如果因为已经到达文件末尾而没有更多的数据，则返回 -1  
                    dos.write(fileByte, 0, length);  
                    dos.flush();     
                }  	
                System.out.println("文件" + f_temp.getName() + "传输完毕");
			}
		}catch(Exception e) {}
		finally {
			fis.close();
		}
	}
	
	//文件字节下载函数
	public void loadFileStream(String path) throws IOException {
		try {
			String loadFileName = dis.readUTF();//输出流读取文件名
			long loadFileLength = dis.readLong();
			len = loadFileLength; 
			//System.out.println(path + File.separatorChar + loadFileName);
			byte[] fileByte = new byte[1024];  
			int length = 0;   
		
			//与系统有关的默认名称分隔符。此字段被初始化为包含系统属性 File.separator 值的第一个字符。
			//在 Microsoft Windows 系统上，它为 '\\'。所以输出为 绝对路径\\文件名
			
			//根据路径和名字新建一个file实例
			//生成文件实例为空，再写入数据
			File f_temp = new File(path + File.separatorChar + loadFileName); 
			System.out.println("文件" + f_temp.getName() + "正在下载");
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