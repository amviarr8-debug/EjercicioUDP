/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package ejercicioudp.fase1;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class ClienteUDP {
    
    // Configuración
    private static final String SERVER_IP = "127.0.0.1"; // IP local para pruebas
    private static final int SERVER_PORT = 9876; 
    private static final int TIMEOUT_MS = 5000; // 5 segundos

    public static void main(String[] args) {
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("Introduce tu nombre: ");
        String nombreCliente = scanner.nextLine();
        
        // 1. Construir la trama de saludo
        String mensajeAEnviar = "@hola#" + nombreCliente + "@";
        
        DatagramSocket clientSocket = null;
        
        try {
            // Crear el socket del cliente
            clientSocket = new DatagramSocket();
            
            // *** APLICACIÓN DEL REQUISITO CLAVE: TIMEOUT ***
            clientSocket.setSoTimeout(TIMEOUT_MS);
            System.out.println("Configurando timeout de " + TIMEOUT_MS / 1000 + " segundos...");

            // Obtener la dirección IP del servidor
            InetAddress IPAddress = InetAddress.getByName(SERVER_IP);
            byte[] sendData = mensajeAEnviar.getBytes();

            // 2. Crear y enviar el paquete
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT);
            clientSocket.send(sendPacket);
            System.out.println("Enviado: " + mensajeAEnviar);

            // 3. Esperar la respuesta
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            
            System.out.println("Esperando respuesta del servidor...");
            
            // Esta línea se bloqueará hasta que reciba el paquete o salte el timeout
            clientSocket.receive(receivePacket);

            // Procesar la respuesta
            String respuestaServidor = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("\n<<< RESPUESTA DEL SERVIDOR >>>");
            System.out.println(respuestaServidor);

        } catch (SocketTimeoutException e) {
            // Excepción lanzada si no llega la respuesta en 5 segundos
            System.err.println("\n!!! ERROR: TIMEOUT !!!");
            System.err.println("El servidor no respondió en " + TIMEOUT_MS / 1000 + " segundos. Paquete perdido o servidor inactivo.");
            
        } catch (IOException e) {
            System.err.println("Error de I/O: " + e.getMessage());
            
        } finally {
            if (clientSocket != null) {
                clientSocket.close(); // Siempre cerrar el socket
                System.out.println("Socket del cliente cerrado.");
            }
        }
    }
}
