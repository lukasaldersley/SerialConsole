import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class SerialConsole implements ActionListener, KeyListener {
	private JFrame mainWindow;
	private Box inputBox;
	private JScrollPane outputScroller;
	private JPanel selectorPanel;

	private JTextField input;
	private JButton sendButton;

	private JTextArea output;

	private JButton clearOutputButton;
	private JButton connectDisconnectButton;
	private JButton refreshButton;
	private JButton saveContents;

	private JComboBox<Integer> baudSelection;
	private JComboBox<String> lineEndingSelection;
	private JComboBox<String> portSelection;
	private JComboBox<String> flowControlSelection;
	private final Integer[] baudrates = { 9600, 19200, 38400, 115200 };
	private final String[] lineEndingNames = { "CRLF (\\r\\n)", "LF (\\n)", "NONE" };
	private final String[] lineEndings = { "\r\n", "\n", "" };
	private final String[] lineEndingCharStrings = { "\\r\\n", "\\n", "" };
	private final int[] flowControlOptions = { SerialPort.FLOW_CONTROL_DISABLED,
			(SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED),
			(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED),
			(SerialPort.FLOW_CONTROL_DSR_ENABLED | SerialPort.FLOW_CONTROL_DTR_ENABLED) };
	private final String[] flowControlOptionStrings = { "NONE", "XON/XOFF", "RTS/CTS", "DSR/DTR" };
	private SerialPort chosenPort;
	private SerialPort[] ports;
	private String[] portnames;
	final JFileChooser FileChooser = new JFileChooser();

	private boolean enabled = false;
	private boolean showTransmissions = true;

	// boolean rxen=false;

	private SerialPortDataListener spdl;

	public static void main(String[] args) {
		Properties properties = System.getProperties();
		// Java 8
		properties.forEach((k, v) -> System.out.println(k + ":" + v));
		if (args.length > 0) {
			if (args[0].equals("UPDATED_LIB")) {
				new SerialConsole(false, true, "", "", args[1], args[2]);
			} else if (args[0].equals("UPDATED_SELF")) {
				new SerialConsole(true, false, args[1], args[2], "", "");
			} else if (args[0].equals("UPDATED_BOTH")) {
				new SerialConsole(true, true, args[1], args[2], args[3], args[4]);
			} else {
				new SerialConsole(false, false, "", "", "", "");
			}
		} else {
			JFrame f = new JFrame();
			f.setUndecorated(true);
			f.add(new JLabel("PLEASE WAIT - SerialMonitor is looking for Updates"));
			f.setSize(300, 60);
			f.validate();
			f.setLocationRelativeTo(null);
			f.setVisible(true);
			checkForLibUpdate();
			f.setVisible(false);
			new SerialConsole(false, false, "", "", "", "");
			f.dispose();
		}
	}

	public void setupSerial() {
		ports = SerialPort.getCommPorts();
		if (ports.length == 0) {
			portnames = new String[1];
			portnames[0] = "NO SERIAL PORTS AVAILABLE";
			connectDisconnectButton.setEnabled(false);
			return;
		}
		portnames = new String[ports.length];
		for (int i = 0; i < ports.length; i++) {
			String nm = ports[i].getPortDescription() + " / " + ports[i].getDescriptivePortName();
			portnames[i] = nm.contains("(COM") || nm.contains("(tty") ? nm
					: nm + " (" + ports[i].getSystemPortName() + ")";
		}
		connectDisconnectButton.setEnabled(true);
	}

	private static void checkForLibUpdate() {
		File f = new File("SerialConsole_lib");
		if (f.exists()) {// if the file doesn't exist, this is runnig straight from an IDE or with an
							// explicit classpath set => "included" library verion doesn't matter => don't
							// attempt to update
			String libDownloadPath = "https://github.com";
			String libVersion = "";

			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						new URL("https://github.com/Fazecast/jSerialComm/releases/latest").openStream()));
				String line = br.readLine();
				while (line != null) {
					if (line.contains(".jar\"")) {
						libDownloadPath += line.substring(line.indexOf("/Fazecast"),
								line.indexOf("\" rel=\"nofollow\""));
						libVersion = libDownloadPath.substring(libDownloadPath.indexOf("jSerialComm-") + 12,
								libDownloadPath.indexOf(".jar"));
						break;
					}
					line = br.readLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;// couldn't get/parse update information => abort check
			}

			boolean newerVersionAvailable = false;
			String oldVersion = SerialPort.getVersion();
			String[] remote = libVersion.split("\\.");
			String[] local = oldVersion.split("\\.");
			for (int i = 0; i < 3; i++) {
				int rm = Integer.parseInt(remote[i]);
				int lc = Integer.parseInt(local[i]);
				System.out.println(remote[i] + "|" + local[i]);
				if (rm > lc) {
					newerVersionAvailable = true;
					break;
				} else if (rm < lc) {
					break;// the local verion is MORE current than the remote version => CERTAINLY no
							// update needed (this isn't going to happen)
				}
			}
			if (newerVersionAvailable) {
				f = new File("SerialConsole_lib/jSerialComm.jar");
				f.renameTo(new File("SerialConsole_lib/jSerialComm_old.jar"));
				f = new File("SerialConsole_lib/jSerialComm_old.jar");// probably unneccessary but I don't really care
				if (Tools.downloadJarfile(libDownloadPath, "SerialConsole_lib/jSerialComm.jar")) {
					f.delete();// dowload succeded => delete old version
					try {
						Runtime.getRuntime()
								.exec("java -jar SerialConsole.jar UPDATED_LIB " + oldVersion + " " + libVersion);
						System.exit(0);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					f.renameTo(new File("SerialConsole_lib/jSerialComm.jar"));// download failed => restore old version
				}
			}
		} else {
			System.out.println("running out of IDE => skipping version checks");
		}
	}

	public void refreshPorts() {
		enabled = false;
		try {
			chosenPort.removeDataListener();
			chosenPort.closePort();
		} catch (NullPointerException ex) {
			;
		} finally {
			chosenPort = null;
		}
		ports = null;
		portnames = null;
		setupSerial();
		portSelection.removeAllItems();
		for (String s : portnames) {
			portSelection.addItem(s);
		}
		int preselectedPort = -1;
		for (int i = 0; i < portSelection.getItemCount(); i++) {
			if (portSelection.getItemAt(i).contains("FT232R")) {
				preselectedPort = i;
				break;
			}
		}
		if (preselectedPort == -1) {
			for (int i = 0; i < portSelection.getItemCount(); i++) {
				if (portSelection.getItemAt(i).contains("CH340")) {
					preselectedPort = i;
					break;
				}
			}
		}
		if (preselectedPort == -1) {
			for (int i = 0; i < portSelection.getItemCount(); i++) {
				if (portSelection.getItemAt(i).contains("Arduino")) {
					preselectedPort = i;
					break;
				}
			}
		}
		if (preselectedPort == -1) {
			preselectedPort = 0;
		}
		portSelection.setSelectedIndex(preselectedPort);
	}

	public SerialConsole(boolean selfUpdated, boolean libUpdated, String selfOldVer, String selfNewVer,
			String libOldVer, String libNewVer) {
		System.out.println("Using jSerialComm verion: " + SerialPort.getVersion());
		/// create and setup top bar
		inputBox = Box.createHorizontalBox();
		inputBox.add(Box.createHorizontalGlue());

		input = new JTextField();
		input.addKeyListener(this);
		inputBox.add(input);
		inputBox.add(Box.createHorizontalGlue());

		sendButton = new JButton("SEND");
		sendButton.addActionListener(this);
		inputBox.add(sendButton);
		inputBox.add(Box.createHorizontalGlue());
		/// end create and setup top bar

		/// create and setup middle area
		output = new JTextArea();
		output.setEditable(false);
		output.addKeyListener(this);
		Font originalFont=output.getFont();
		// System.out.println(originalFont.getFontName());
		// System.out.println(originalFont.getStyle());
		// System.out.println(originalFont.getSize());
		//the default font is fine, but it's really quite small, so I increase it from 12 to 18 here
		output.setFont(new Font(originalFont.getName(),originalFont.getStyle(),18));

		outputScroller = new JScrollPane(output);
		outputScroller.addKeyListener(this);
		new SmartScroller(outputScroller);// taken from https://tips4java.wordpress.com/2013/03/03/smart-scrolling/
		/// end create and setup middle area

		/// create and setup lower controls
		portSelection = new JComboBox<String>();
		portSelection.addActionListener(this);

		refreshButton = new JButton("Reload Ports");
		refreshButton.addActionListener(this);

		baudSelection = new JComboBox<Integer>(baudrates);
		baudSelection.addActionListener(this);

		lineEndingSelection = new JComboBox<String>(lineEndingNames);
		lineEndingSelection.addActionListener(this);

		flowControlSelection = new JComboBox<String>(flowControlOptionStrings);
		flowControlSelection.setSelectedIndex(1);
		flowControlSelection.addActionListener(this);

		saveContents = new JButton("SAVE");
		saveContents.addActionListener(this);

		clearOutputButton = new JButton("CLEAR");
		clearOutputButton.addActionListener(this);

		connectDisconnectButton = new JButton("CONNECT");
		connectDisconnectButton.addActionListener(this);
		enabled = false;
		/// end create and setup lower controls

		/// create and setup lower panel
		JPanel selectorPanelUpper = new JPanel();
		selectorPanelUpper.setLayout(new FlowLayout());
		selectorPanelUpper.add(portSelection);
		selectorPanelUpper.add(refreshButton);
		selectorPanelUpper.add(saveContents);
		selectorPanelUpper.add(clearOutputButton);
		selectorPanelUpper.add(connectDisconnectButton);

		JPanel selectorPanelLower = new JPanel();
		selectorPanelLower.setLayout(new FlowLayout());
		selectorPanelLower.add(baudSelection);
		baudSelection.setSelectedIndex(3);
		selectorPanelLower.add(lineEndingSelection);
		selectorPanelLower.add(flowControlSelection);

		selectorPanel = new JPanel();
		selectorPanel.setLayout(new BoxLayout(selectorPanel, BoxLayout.Y_AXIS));
		selectorPanel.add(selectorPanelUpper);
		selectorPanel.add(selectorPanelLower);
		selectorPanel.addKeyListener(this);

		/// end create and setup lower panel

		/// gather information on available serial ports
		setupSerial();
		// for (String s : portnames) {
		// portSelection.addItem(s);
		// }
		refreshPorts();
		/// end gather information on available serial ports

		/// create and setup main window
		mainWindow = new JFrame("SerialMonitor by Lukas Aldersley");
		mainWindow.add(inputBox, BorderLayout.PAGE_START);
		mainWindow.add(outputScroller, BorderLayout.CENTER);
		mainWindow.add(selectorPanel, BorderLayout.PAGE_END);

		mainWindow.setSize(new Dimension(1000, 600));
		// mainWindow.validate();
		mainWindow.setVisible(true);
		mainWindow.setLocationRelativeTo(null);
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainWindow.addKeyListener(this);
		/// end create and setup main window

		spdl = new SerialPortDataListener() {

			@Override
			public int getListeningEvents() {
				return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
			}

			@Override
			public void serialEvent(SerialPortEvent event) {
				if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
					return;
				}
				// if(rxen){
					int avail=chosenPort.bytesAvailable();
				byte[] newData = new byte[avail];
				int reallyread=chosenPort.readBytes(newData, chosenPort.bytesAvailable());//attention this sort of dies every now and then if I send a load of stuff to it
				// System.out.println("Read " + numRead + " bytes.");
				if(reallyread!=avail){
					System.out.println("Missmatch between read and expected byte number in serialEvent");
				}
				for (byte b : newData) {
					append((char) b);
				}
				// }
			}
		};

		if (selfUpdated) {
			append("--Updated SerialConsole from version " + selfOldVer + " to version " + selfNewVer + " --\r\n");
		}
		if (libUpdated) {
			append("--Updated jSerialComm library from version " + libOldVer + " to version " + libNewVer + " --\r\n");
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == sendButton) {
			send();
		} else if (e.getSource() == clearOutputButton) {
			clear();
		} else if (e.getSource() == refreshButton) {
			reload();
		} else if (e.getSource() == connectDisconnectButton) {
			connectDisconnect();
		}
		else if(e.getSource()==saveContents){
			Save();
		}
	}

	private void connectDisconnect() {
		if (enabled) {
			disconnect();
		} else {
			chosenPort = ports[portSelection.getSelectedIndex()];
			append("--connecting to " + portnames[portSelection.getSelectedIndex()] + " @ "
					+ baudSelection.getSelectedItem() + " baud with flowcontrol: "
					+ flowControlOptionStrings[flowControlSelection.getSelectedIndex()] + "--\r\n");
			chosenPort.setBaudRate((int) baudSelection.getSelectedItem());
			chosenPort.setFlowControl(flowControlOptions[flowControlSelection.getSelectedIndex()]);
			chosenPort.setNumDataBits(8);
			chosenPort.setNumStopBits(1);
			chosenPort.setParity(SerialPort.NO_PARITY);
			chosenPort.openPort();
			// chosenPort.writeBytes(new byte[]{'T','S','T'}, 3);
			chosenPort.addDataListener(spdl);
			enabled = true;
			connectDisconnectButton.setText("DISCONNECT");
		}
	}

	private void reload() {
		disconnect();
		if (bytesSinceLineBreak > 0) {
			append("\r\n");
		}
		append("--reloading available Serial Ports--\r\n");
		refreshPorts();
	}

	private void clear() {
		output.setText("");
	}

	private void disconnect() {
		try {
			chosenPort.removeDataListener();
			chosenPort.closePort();
		} catch (Exception ex) {
			;
		} finally {
			chosenPort = null;
		}
		if (bytesSinceLineBreak > 0) {
			append("\r\n");
		}
		append("--disconnecting--\r\n");
		enabled = false;
		connectDisconnectButton.setText("CONNECT");
	}

	int bytesSinceLineBreak = 0;

	void append(String str) {
		for (char c : str.toCharArray()) {
			append(c);
		}
	}

	void append(char c) {
		output.append(String.valueOf(c));
		if (c == '\n') {
			bytesSinceLineBreak = 0;
		} else {
			bytesSinceLineBreak++;
		}
	}

	private void send() {
		/*
		 * if(input.getText().equals("RXEN=TRUE;")){ rxen=true; }
		 * if(input.getText().equals("RXEN=FALSE;")){ rxen=false; }
		 */
		if (chosenPort != null && enabled) {
			char[] c = (input.getText() + lineEndings[lineEndingSelection.getSelectedIndex()]).toCharArray();
			byte[] b = new byte[c.length];
			for (int i = 0; i < c.length; i++) {
				b[i] = (byte) c[i];
			}
			if (showTransmissions) {
				System.out.println(bytesSinceLineBreak);
				if (bytesSinceLineBreak > 0) {
					append("\r\n");
				}
				append("[TX: \"" + input.getText() + lineEndingCharStrings[lineEndingSelection.getSelectedIndex()]
						+ "\"]\r\n");
			}
			chosenPort.writeBytes(b, b.length);
			input.setText("");
		} else {
			append("--please connect to a device first--\r\n");
		}
	}

	private void Save() {
		int returnVal = FileChooser.showSaveDialog(null);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			BufferedWriter bw = null;
			try {
				File file = FileChooser.getSelectedFile();
				file.delete();
				file.createNewFile();
				bw = new BufferedWriter(new FileWriter(file));
				bw.write(output.getText());
				bw.flush();
			} catch (Exception ex) {
				if (bytesSinceLineBreak > 0) {
					append("\r\n");
				}
				append("--ERROR saving--\r\n");
			} finally {
				try {
					bw.close();
				} catch (Exception ec) {// some really bad error
					bw = null;
				}
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getSource() == input && e.getKeyCode() == KeyEvent.VK_ENTER && chosenPort != null) {
			send();
		} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_DELETE) {
			System.out.println("Ctrl+Entf");
			clear();
		} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
			System.out.println("Ctrl+S");
			Save();
		} else if (e.isControlDown() && e.isAltDown() && e.getKeyCode() == KeyEvent.VK_C) {
			System.out.println("Ctrl+Alt+C");
			connectDisconnect();
		} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_R) {
			System.out.println("Ctrl+R");
			reload();
		}
		else if(e.getKeyCode()==KeyEvent.VK_UP){
			//recall last
		}
		else if(e.getKeyCode()==KeyEvent.VK_DOWN){
			if(e.isControlDown()){
				//jump to last
			}
			else{
				//undo recall
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
}
