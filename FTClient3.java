import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.table.*;
class FileUploadEvent
{
    private String uplodeId;
    private File file;
    private long numberOfBytesUploaded;

    public FileUploadEvent()
    {
        this.uplodeId=null;
        this.file=null;
        this.numberOfBytesUploaded=0;
    }
    public void setUploadeId(String uplodeId)
    {
        this.uplodeId=uplodeId;        
    }
    public String getUploadeId()
    {
        return this.uplodeId;
    }
    public void setFile(File file)
    {
        this.file=file;        
    }
    public File getFile()
    {
        return this.file;
    }
    public void setNumberOfBytesUploaded(long numberOfBytesUploaded)
    {
        this.numberOfBytesUploaded=numberOfBytesUploaded;        
    }
    public long getNumberOfBytesUploaded()
    {
        return this.numberOfBytesUploaded;
    }
}
interface FileUploadeListner
{
    public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent);
}
class FileModel extends AbstractTableModel
{
    private ArrayList<File> files;
    FileModel()
    {
        this.files=new ArrayList<>(); 
    }
    public ArrayList<File> getFile()
    {
        return files;
    }
    public int getRowCount() {
        return this.files.size();
    }
    public int getColumnCount() {
        return 2;
    }
    public String getColumnName(int c)
    {
        if(c==0) return "S.No";
        return "File"; 
    }
    public Class getColumnClass(int c)
    {
        if(c==0) return Integer.class;
        return String.class; 
    }
    public boolean isCellEditable(int r,int c)
    {
        return false;
    }   
    public Object getValueAt(int r, int c) {
        if(c==0) return r+1;
        return this.files.get(r).getAbsolutePath();
    }
    public void add(File file)
    {
        this.files.add(file);
        fireTableDataChanged();
    }
}

class FTClientFrame extends JFrame
{
    private String host;
    private int portNumber;
    private FileSelectionPanel fileSelectionPanel;
    private FileUplodeViewPanel fileUplodeViewPanel;
    private Container container;
    FTClientFrame(String host,int portNumber)
    {
        this.host=host;
        this.portNumber=portNumber;
        fileSelectionPanel=new FileSelectionPanel();
        fileUplodeViewPanel = new FileUplodeViewPanel();
        container=getContentPane();
        container.setLayout(new GridLayout(1,2));
        container.add(fileSelectionPanel);
        container.add(fileUplodeViewPanel);
        setSize(1200,600);
        setLocation(10,20);
        setVisible(true);
    }

 class FileSelectionPanel extends JPanel implements ActionListener
 {
    private JLabel titleLable;
    private FileModel model;
    private JTable table;
    private JScrollPane jsp;
    private JButton addFileButton;

    FileSelectionPanel()
    {
        setLayout(new BorderLayout());
        titleLable= new JLabel("Select File");
        model = new FileModel();
        table= new JTable(model);
        jsp=new JScrollPane(table,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        addFileButton= new JButton("Add File");
        addFileButton.addActionListener(this);
        add(titleLable,BorderLayout.NORTH);
        add(jsp,BorderLayout.CENTER);
        add(addFileButton,BorderLayout.SOUTH);
    }
    public ArrayList<File> getFile()
    {
        return model.getFile();
    }
    public void actionPerformed(ActionEvent ev) {
        JFileChooser jfc= new JFileChooser();
        jfc.setCurrentDirectory(new File("."));
        int selectedOption=jfc.showOpenDialog(this);
        if(selectedOption==jfc.APPROVE_OPTION)
        {
            File selectedFile =jfc.getSelectedFile();
            model.add(selectedFile);
        }
    }
}

class FileUplodeViewPanel extends JPanel implements ActionListener,FileUploadeListner
{

    private JButton uploadeFileButton;
    private JPanel progressPanelsContainer;
    private JScrollPane jsp;
    private ArrayList<ProgressPanel> progressPanels;
    private ArrayList<FileUplodeThread> fileUploaders;
    ArrayList<File> files;

    FileUplodeViewPanel()
    {
        uploadeFileButton= new JButton("Uplode File");
        setLayout(new BorderLayout());
        add(uploadeFileButton,BorderLayout.NORTH);
        uploadeFileButton.addActionListener(this);
    }
    public void actionPerformed(ActionEvent ev) {
       files=fileSelectionPanel.getFile();
       if(files.size()==0)
        {
            JOptionPane.showMessageDialog(FTClientFrame.this,"No files selected to uploade");
            return;
        }
        progressPanelsContainer = new JPanel();
        progressPanelsContainer.setLayout(new GridLayout(files.size(),1));
        ProgressPanel progressPanel;
        progressPanels = new ArrayList<>();
        fileUploaders=new ArrayList<>();
        FileUplodeThread fut;
        String uploaderId;
        for(File file :files)
        {
            uploaderId=UUID.randomUUID().toString();
            progressPanel = new ProgressPanel(uploaderId,file);
            progressPanels.add(progressPanel);
            progressPanelsContainer.add(progressPanel);
            fut=new FileUplodeThread(this,uploaderId, file, host, portNumber);
            fileUploaders.add(fut);
        }
        jsp = new JScrollPane(progressPanelsContainer,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(jsp,BorderLayout.CENTER);
        this.revalidate();
        this.repaint();
        for(FileUplodeThread fileUplodeThread : fileUploaders)
        {
            fileUplodeThread.start();
        }
    }
    public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent) 
    {
        String uploadedId = fileUploadEvent.getUploadeId();
        long numberOfBytesUploaded = fileUploadEvent.getNumberOfBytesUploaded();
        File file =fileUploadEvent.getFile();
        for(ProgressPanel progressPanel : progressPanels)
        {
            if(progressPanel.getId().equals(uploadedId))
            {
                progressPanel.updateProgressBar(numberOfBytesUploaded);
                break;
            }
        }
    }
    class ProgressPanel extends JPanel
    {
        public File file;
        private JLabel fileNamLabel;
        private JProgressBar progressBar;
        private long fileLength;
        private String id;
        
        public ProgressPanel(String id,File file)
        {
            this.id=id;
            this.file=file;
            this.fileLength=file.length();
            fileNamLabel=new JLabel("Uploading :"+file.getAbsoluteFile());
            progressBar= new JProgressBar(1,100);
            setLayout(new GridLayout(2,1));
            add(fileNamLabel);
            add(progressBar);
        }
        public String getId()
        {
            return id;
        }
        private void updateProgressBar(long bytesUploaded)
        {
            int percentage;
            if(bytesUploaded==fileLength) percentage=100;
            else
            percentage=(int)((bytesUploaded * 100)/fileLength);
            progressBar.setValue(percentage);
            if(percentage==100)
            {
                fileNamLabel.setText("Uploaded : "+ file.getAbsolutePath());
            }
        }
    }
}
public static void main(String[] args) {
    FTClientFrame fcf = new FTClientFrame("localhost",5500);   
}
}

class FileUplodeThread extends Thread 
{
    private String id;
    private File file;
    private String host;
    private int portNumber;
    private FileUploadeListner fileUploadeListner;
    FileUplodeThread(FileUploadeListner fileUploadeListner,String id,File file,String host,int portNumber)
    {
        this.fileUploadeListner=fileUploadeListner;
        this.id=id;
        this.file=file;
        this.host=host;
        this.portNumber=portNumber;
    }
    public void run() {
        try {
            long lengthOfFile=file.length();
            String name = file.getName();
            byte header[] = new byte[1024];
            long k,x;
            int i;
            i=0;
            k=lengthOfFile;
            while(k>0)
            {
                header[i] = (byte)(k%10);
                k=k/10;
                i++;
            }
            header[i]=(byte)',';
            i++;
            x=name.length();
            int r=0;
            while(r<x)
            {
                header[i]=(byte)name.charAt(r);
                i++;
                r++;
            }
            while(i<=1023)
            {
                header[i]=(byte)32;
                i++;
            }


            Socket socket = new Socket("localhost", 5500);
            OutputStream os = socket.getOutputStream();
            os.write(header, 0, 1024);
            os.flush();
            InputStream is = socket.getInputStream();
            byte[] ack = new byte[1];
            int bytereadCount;
            while (true) {
                bytereadCount = is.read(ack);
                if (bytereadCount == -1)
                    continue;
                break;
            }
            FileInputStream fis = new FileInputStream(file);
            int chunkSize = 4096;
            byte bytes[] = new byte[chunkSize];
            int j = 0;
            while (j < lengthOfFile) {
                bytereadCount= fis.read(bytes);
                os.write(bytes, 0, bytereadCount);
                os.flush();
                j = j + bytereadCount;
                long brc = j;
                SwingUtilities.invokeLater(()->{
                    FileUploadEvent fue = new FileUploadEvent();
                    fue.setUploadeId(id);
                    fue.setFile(file);
                    fue.setNumberOfBytesUploaded(brc);
                    fileUploadeListner.fileUploadStatusChanged(fue);
                });
            }
            fis.close();
            while (true) {
                bytereadCount = is.read(ack);
                if (bytereadCount == -1)
                    continue;
                break;
            }
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}