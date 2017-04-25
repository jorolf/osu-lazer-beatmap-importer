import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

public class BeatmapConverter extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JFileChooser chooser;
	private File tempDir;

	public BeatmapConverter() {
		setTitle("Osu! Beatmap Converter");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(370, 150);
		setLayout(null);
		setResizable(false);

		JTextField oldDir = new JTextField(new File(System.getenv("appdata")+"\\..\\Local\\osu!\\Songs").getAbsolutePath());
		oldDir.setToolTipText("Old osu! song directory");
		oldDir.setBounds(0, 0, 300, 30);
		add(oldDir);

		JProgressBar progress = new JProgressBar();
		progress.setBounds(0, 90, 300, 30);
		progress.setStringPainted(true);
		add(progress);

		JTextField newDir = new JTextField(new File(System.getenv("appdata")+"\\..\\Local\\osulazer\\osu!.exe").getAbsolutePath());
		newDir.setToolTipText("New osu! song directory");
		newDir.setBounds(0, 60, 300, 30);
		add(newDir);

		JButton oldDirBrowse = new JButton("Browse");
		oldDirBrowse.setBounds(300, 0, 70, 30);
		oldDirBrowse.setMargin(new Insets(0, 0, 0, 0));
		oldDirBrowse.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				chooser.setDialogTitle("Select osu!stable song directory");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (chooser.showOpenDialog(oldDirBrowse) == JFileChooser.APPROVE_OPTION)
					oldDir.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		});
		add(oldDirBrowse);

		JButton tempDirBrowse = new JButton("TempDir");
		tempDirBrowse.setBounds(300, 30, 70, 30);
		tempDirBrowse.setMargin(new Insets(0, 0, 0, 0));
		tempDirBrowse.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				chooser.setDialogTitle("Select temporary song directory");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (chooser.showOpenDialog(tempDirBrowse) == JFileChooser.APPROVE_OPTION)
					tempDir = chooser.getSelectedFile();
			}
		});
		add(tempDirBrowse);

		JButton newDirBrowse = new JButton("Browse");
		newDirBrowse.setBounds(300, 60, 70, 30);
		newDirBrowse.setMargin(new Insets(0, 0, 0, 0));
		newDirBrowse.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				chooser.setDialogTitle("Select osu!lazer executable");
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				if (chooser.showOpenDialog(newDirBrowse) == JFileChooser.APPROVE_OPTION)
					newDir.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		});
		add(newDirBrowse);

		JButton convert = new JButton("Convert");
		convert.setBounds(0, 30, 300, 30);
		convert.setMargin(new Insets(0, 0, 0, 0));
		convert.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (oldDir.getText().isEmpty() || newDir.getText().isEmpty()) {
					JOptionPane.showMessageDialog(convert,
							"Please select your osu!stable song directory and your osu!lazer executable!", "ERROR",
							JOptionPane.ERROR_MESSAGE);
				} else {
					Thread t = new Thread(new Runnable() {

						@Override
						public void run() {
							try{
								if((e.getModifiers() & ActionEvent.SHIFT_MASK) == 0)
									zip(new File(oldDir.getText()),tempDir,progress);
							}catch(Exception e){}
							if((e.getModifiers() & ActionEvent.ALT_MASK) == 0){
								List<File> paths;
								if(tempDir == null)
									paths = Arrays.asList(new File(oldDir.getText()).listFiles((dir,name) -> name.endsWith(".osz")));
								else
									paths = Arrays.asList(tempDir.listFiles((dir,name) -> name.endsWith(".osz")));
								List<String> path = new ArrayList<>();
								path.add(newDir.getText());
								for(File oszPath : paths){
									if(path.size() >= 300){
										try {
											new ProcessBuilder(path).start();
											path.clear();
											path.add(newDir.getText());
										} catch (IOException e) {}
									}
									path.add(oszPath.getAbsolutePath());
								}
								try {
									JOptionPane.showMessageDialog(convert, "Everything converted, osu!lazer should start now!", "YAY",
											JOptionPane.INFORMATION_MESSAGE);
									new ProcessBuilder(path).start().waitFor();
								} catch (IOException | InterruptedException e) {e.printStackTrace();}
							}

						}
					});
					t.start();
				}
			}
		});
		add(convert);

		initChooser();

		setVisible(true);
	}

	protected void zip(File from, File temp, JProgressBar bar) throws IOException {
		bar.setMinimum(0);
		bar.setMaximum(from.listFiles(file -> file.isDirectory()).length);
		int i = 0;
		for (File dir : from.listFiles(file -> file.isDirectory())) {
			ZipOutputStream zipStream;
			if(temp == null)
				zipStream = new ZipOutputStream(new FileOutputStream(dir.getAbsolutePath()+".osz"));
			else 
				zipStream = new ZipOutputStream(new FileOutputStream(temp.getAbsolutePath()+"\\"+dir.getName()+".osz"));
			zipDir(dir,zipStream,escapeMetaCharacters(dir.getAbsolutePath()+"\\"));
			zipStream.close();
			i++;
			bar.setValue(i);
			bar.repaint();
		}
	}

	private void zipDir(File directory, ZipOutputStream zipStream, String startDir) throws IOException{
		for(File file: directory.listFiles()){

			ZipEntry entry = new ZipEntry(file.getAbsolutePath().replaceFirst(startDir, ""));
			if(file.isDirectory()){
				zipDir(file, zipStream, startDir);
			}else{
				zipStream.putNextEntry(entry);
				FileInputStream fileStream = new FileInputStream(file);
				byte[] buffer = new byte[1000];
				int len;
				while ((len = fileStream.read(buffer)) > 0){
					zipStream.write(buffer,0,len);
				}
				fileStream.close();
				zipStream.closeEntry();
			}
		}
	}

	public String escapeMetaCharacters(String inputString){
		final String[] metaCharacters = {"\\","^","$","{","}","[","]","(",")",".","*","+","?","|","<",">","-","&"};
		String outputString="";
		for (int i = 0 ; i < metaCharacters.length ; i++){
			if(inputString.contains(metaCharacters[i])){
				outputString = inputString.replace(metaCharacters[i],"\\"+metaCharacters[i]);
				inputString = outputString;
			}
		}
		return outputString;
	}

	private void initChooser() {
		chooser = new JFileChooser();
		chooser.setFileFilter(null);
		chooser.setMultiSelectionEnabled(false);
	}

	public static void main(String[] args) {
		new BeatmapConverter();
	}
}
