/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorremoto;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Servidorremoto extends Thread {

    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    public static byte[] retornaImagem(BufferedImage originalImage) {
        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(originalImage, "jpg", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();
            return imageInByte;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public ServerSocket server = null;
    public boolean aux;

    public int locX;
    public int locY;

    private int x1;
    private int x2;
    private int y1;
    private int y2;
    private int cont = 0;

    private Marcador marcador;

    class recebeComandos extends Thread {

        ObjectInputStream entrada = null;
        ObjectOutputStream saida = null;
        Robot robo = null;
        Socket s = null;

        public recebeComandos(Socket s) {
            this.s = s;
            try {
                robo = new Robot();
                entrada = new ObjectInputStream(s.getInputStream());
                saida = new ObjectOutputStream(s.getOutputStream());

            } catch (IOException | AWTException ex) {
                Logger.getLogger(Servidorremoto.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String comando = entrada.readUTF();

                    if (comando.contains("imagem")) {
                        int x = entrada.readInt();
                        int y = entrada.readInt();

                        Image img
                                = robo.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())).
                                        getScaledInstance(x, y, Image.SCALE_FAST);
                        saida.writeObject(retornaImagem(toBufferedImage(img)));
                        saida.flush();
                    } else if (comando.contains("mousemove")) {
                        int tL = entrada.readInt();
                        int aH = entrada.readInt();

                        int x = entrada.readInt();
                        int y = entrada.readInt();

                        int dW = Toolkit.getDefaultToolkit().getScreenSize().width;
                        int dH = Toolkit.getDefaultToolkit().getScreenSize().height;

                        robo.mouseMove(((x * dW) / tL), ((y * dH) / aH));

                        locX = ((x * dW) / tL);
                        locY = ((y * dH) / aH);
                    } else if (comando.contains("mouseclick")) {
                        int mouse = entrada.readInt();
                        if (mouse == 0) {
                            robo.mousePress(InputEvent.BUTTON1_MASK);
                            robo.mouseRelease(InputEvent.BUTTON1_MASK);
                        } else {
                            robo.mousePress(InputEvent.BUTTON3_MASK);
                            robo.mouseRelease(InputEvent.BUTTON3_MASK);
                        }
                        if (aux == true) {
                            aux = false;
                            if (cont == 0) {
                                x1 = locX;
                                y1 = locY;
                                cont++;
                            } else {
                                x2 = locX;
                                y2 = locY;
//                                marcador = new Marcador(x1, y2);
//                                marcador.setLocation(x2 - x1, y2 - y1);
//                                marcador.setSize(x1, y2);
//                                marcador.setVisible(true);
                                JFrame frame = new JFrame();
                                frame.setLocation(x2 - x1, y2 - y1);
                                frame.setSize(x1, y2);
                                frame.getContentPane().setBackground(Color.PINK);
                                frame.setVisible(true);
                                cont = 0;
                            }

//                            Marcos R. Morais
//                            JFrame frame = new JFrame();
//                            frame.setUndecorated(true);
//                            frame.setVisible(true);
//                            frame.setLocation(locX, locY);
//                            frame.setSize(100, 15);
//                            frame.getContentPane().setBackground(Color.PINK);
//                            Marcador m = new Marcador(locX, locY);
                        }

                    } else if (comando.contains("keypressed")) {
                        int key = entrada.readInt();
                        robo.keyPress(key);
                        robo.keyRelease(key);

                    } else if (comando.contains("cmd")) {
                        Runtime.getRuntime().exec(entrada.readUTF());

                    } else if (comando.contains("receberaquivo")) {
                        recebendoArquivo();
                    } else if (comando.contains("marcador")) {
                        aux = true;
                    }
                    //Açoes do robo
                } catch (IOException ex) {
                    Logger.getLogger(Servidorremoto.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Servidorremoto.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        public synchronized void recebendoArquivo() throws IOException, ClassNotFoundException {

            //2
            byte objectAsByte[] = (byte[]) entrada.readObject();

            //3
            Arquivo arquivo = (Arquivo) getObjectFromByte(objectAsByte);

            //4
            String dir = arquivo.getDiretorioDestino().endsWith("/") ? arquivo
                    .getDiretorioDestino() + arquivo.getNome() : arquivo
                    .getDiretorioDestino() + "/" + arquivo.getNome();
            System.out.println("Escrevendo arquivo " + dir);

            try ( //5
                    FileOutputStream fos = new FileOutputStream(dir)) {
                fos.write(arquivo.getConteudo());
            }

        }

    }

    @Override
    public void run() {
        while (true) {

            try {
                server = new ServerSocket(3312);

                while (true) {
                    new recebeComandos(server.accept()).start();
                }
            } catch (IOException ex) {
                Logger.getLogger(Servidorremoto.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
        }
    }

    private static Object getObjectFromByte(byte[] objectAsByte) {
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(objectAsByte);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();

            bis.close();
            ois.close();

        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
        }
        // TODO Auto-generated catch block

        return obj;

    }

    public static void main(String args[]) {

        new Servidorremoto().start();
    }
}
