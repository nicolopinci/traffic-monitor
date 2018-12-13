package prog;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.*;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import jxl.read.biff.BiffException;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.ArrayList;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


public class ApplicazioneClient implements Runnable{

	private ApplicazioneMobile applicazione;
	private IGestoreApplicazioni server;
	private boolean finito=true;
	

	public ApplicazioneClient(ApplicazioneMobile applicazione) {
		this.applicazione=applicazione;
	}
	
	private void segnalaCoda() throws BiffException, IOException  {

		this.applicazione.setPosizione(this.applicazione.getSensore().rilevaPosizione());
		NotificaApplicazione notifica=new NotificaApplicazione(this.applicazione.getUsernameUtente(), this.applicazione.getPosizione(), "M10 Coda");
		server.segnalaDatabase(notifica);

	}
	
	public boolean loginGrafico(int id) throws RemoteException {

		// basato su http://www.zentut.com/java-swing/simple-login-dialog/

		final JFrame frame = new JFrame("Accesso all'applicazione mobile");
		final JButton btnLogin = new JButton("Login");
		final JButton btnRegistrazione = new JButton("Registrazione");
		

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(300, 100);
		frame.setLayout(new FlowLayout());
		frame.getContentPane().add(btnLogin);
		frame.getContentPane().add(btnRegistrazione);
		frame.setVisible(true);

		btnLogin.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						LoginDialog loginDlg = new LoginDialog(frame, "U", server);
						loginDlg.setVisible(true);

						if(loginDlg.isSucceeded()){
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
							if(registrazioneDlg.isSucceeded()){
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

		this.applicazione.getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.applicazione.getFrame().pack();
		this.applicazione.getFrame().setVisible(true);


	}
	
	private void pulisciNotifiche() {
		this.applicazione.getAreaNotifiche().setText("");
	}

	public boolean finito() {
		
		return this.finito;
	}
	
	public void run(){
		
try {
			
			//System.setSecurityManager(new SecurityManager());
			
			
			customSecurityManager cSM = new customSecurityManager(System.getSecurityManager());
			   System.setSecurityManager(cSM);

			Registry registry = LocateRegistry.getRegistry("127.0.0.1", 12345);

			this.server = (IGestoreApplicazioni) registry.lookup("gestApp");
			
			this.applicazione.setIdentificativo(this.server.getIdApp());
			

			this.finito=false;
		
			loginGrafico(this.applicazione.getIdentificativo());

	
}catch (Exception e) {
	e.printStackTrace(System.err);
	System.out.println("HelloClient exception: " + e);
	}

	}

}