package prog;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.rmi.*;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import jxl.read.biff.BiffException;
import java.rmi.registry.*;


public class ApplicazioneClient implements Runnable {

	private ApplicazioneMobile applicazione;
	private IGestoreApplicazioni server;


	public ApplicazioneClient(ApplicazioneMobile applicazione) {
		this.applicazione=applicazione;
	}

	private void segnalaCoda() throws BiffException, IOException  {

		if (!applicazione.getFissa()) {
			this.applicazione.setPosizione(this.applicazione.getSensore().rilevaPosizione());
		}
		NotificaApplicazione notifica=new NotificaApplicazione(this.applicazione.getUsernameUtente(), this.applicazione.getPosizione(), "M10 Coda");
		server.segnalaDatabase(notifica);

	}

	public boolean loginGrafico(int id) throws RemoteException {

		// basato su http://www.zentut.com/java-swing/simple-login-dialog/

		final JFrame frame = new JFrame("Accesso all'applicazione mobile");
		final JButton btnLogin = new JButton("Login");
		final JButton btnRegistrazione = new JButton("Registrazione");


		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(260, 100);
		frame.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 20));
		frame.getContentPane().add(btnLogin);
		frame.getContentPane().add(btnRegistrazione);
		frame.setVisible(true);


		btnLogin.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						LoginDialog loginDlg = new LoginDialog(frame, "U", server);
						loginDlg.setVisible(true);

						if(loginDlg.loginRiuscito()){
							loginDlg.setVisible(false);
							frame.setVisible(false);

							try {
								applicazione.setUtente(server.passaggioUtente(loginDlg.getUsername()));
								try {
									server.aggiungiApplicazione(applicazione.getIdentificativo(), applicazione.getUtente());
								} catch (RemoteException e3) {
									// TODO Auto-generated catch block
									e3.printStackTrace();
								}
							} catch (RemoteException e2) {
								// TODO Auto-generated catch block
								e2.printStackTrace();
							}								

							try {
								mostraGUI(id);
							} catch (BiffException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						}
					}
				});



		btnRegistrazione.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						RegistrazioneDlg registrazioneDlg = new RegistrazioneDlg(frame, "U", server);
						registrazioneDlg.setVisible(true);


						System.out.println(registrazioneDlg.getUsername());
						if(registrazioneDlg.registrazioneRiuscita()){
							registrazioneDlg.setVisible(false);
							frame.setVisible(false);

							applicazione.setUtente ( new Utente(registrazioneDlg.getUsername(), registrazioneDlg.getPassword()));
							try {
								server.aggiungiApplicazione(applicazione.getIdentificativo(), applicazione.getUtente());
							} catch (RemoteException e3) {
								// TODO Auto-generated catch block
								e3.printStackTrace();
							}
							try {
								server.registraUtente(applicazione.getUtente());
							} catch (RemoteException e2) {
								// TODO Auto-generated catch block
								e2.printStackTrace();
							}

							try {
								mostraGUI(id);
							} catch (BiffException | IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				});


		return !(frame.isVisible());

	}

	public void logout() throws RemoteException {
		this.applicazione.setUtente(null);
		server.rimuoviApplicazione(this.applicazione.getIdentificativo());
		this.applicazione.getFrame().setVisible(false);
		this.loginGrafico(this.applicazione.getIdentificativo());
	}

	public void mostraGUI(int id) throws BiffException, IOException {
		JButton segnalaCodaBtn = new JButton("Segnala coda");
		JButton svuotaNotificheBtn = new JButton("Svuotare area delle notifiche");
		JButton logoutBtn = new JButton("Logout");
		JCheckBox fissaPosizione = new JCheckBox("Selezionare per fissare la posizione");

		this.applicazione.setFrame(new JFrame());
		this.applicazione.getFrame().setTitle("Applicazione mobile");

		// da https://coderanch.com/t/341045/java/expand-JTextArea-main-panel-resized
		// https://stackoverflow.com/questions/33100147/how-to-set-window-size-without-extending-jframe

		this.applicazione.getPaneNotifiche().setPreferredSize(new Dimension(175,150));

		JPanel jp = new JPanel(new BorderLayout());
		JPanel top = new JPanel();
		JPanel bottom = new JPanel();

		JPanel left = new JPanel();
		JPanel right = new JPanel();
		jp.add(top,BorderLayout.NORTH);
		jp.add(bottom,BorderLayout.SOUTH);
		jp.add(left,BorderLayout.WEST);
		jp.add(right,BorderLayout.EAST);
		jp.add(this.applicazione.getPaneNotifiche(),BorderLayout.CENTER);
		this.applicazione.getFrame().getContentPane().add(jp);

		segnalaCodaBtn.addActionListener(e -> {
			try {
				segnalaCoda();
			} catch (BiffException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		bottom.add(segnalaCodaBtn);

		logoutBtn.addActionListener(e -> {
			try {
				logout();
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		bottom.add(logoutBtn);

		svuotaNotificheBtn.addActionListener(e -> pulisciNotifiche());

		bottom.add(svuotaNotificheBtn);
		top.add(fissaPosizione);

		fissaPosizione.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						applicazione.setFissa(fissaPosizione.isSelected());
					}
				});


		this.applicazione.getFrame().addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try {
					logout();
				} catch (RemoteException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});

		this.applicazione.getFrame().pack();
		this.applicazione.getFrame().setVisible(true);

	}

	private void pulisciNotifiche() {
		this.applicazione.getAreaNotifiche().setText("");
	}

	public void run(){

		try {

			customSecurityManager cSM = new customSecurityManager(System.getSecurityManager());
			System.setSecurityManager(cSM);

			Registry registry = LocateRegistry.getRegistry("127.0.0.1", 12345);

			this.server = (IGestoreApplicazioni) registry.lookup("gestApp");

			this.applicazione.setIdentificativo(this.server.getIdApp());

			Thread t2=new Thread(new ApplicazioneServer(this.applicazione));
			t2.start();

			loginGrafico(this.applicazione.getIdentificativo());

		} catch (Exception e) {
			  JOptionPane.showMessageDialog(null,
				        "Il sistema centrale non è disponibile.\nI dati possono essere trasmessi solo in presenza\ndi una connessione con il sistema centrale.",
				        "Errore di connessione",
				        JOptionPane.ERROR_MESSAGE);
		}

	}

}