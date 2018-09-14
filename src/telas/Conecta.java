/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package telas;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 *
 * @author Guilherme Costa
 */
public class Conecta {

    public Socket s = null;
    public ObjectInputStream recebe = null;
    public ObjectOutputStream envia = null;

    public Conecta(String host, int porta) throws IOException {
        s = new Socket(host, porta);
        envia = new ObjectOutputStream(s.getOutputStream());
        recebe = new ObjectInputStream(s.getInputStream());

    }
}
