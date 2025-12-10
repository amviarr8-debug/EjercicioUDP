/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ejercicioudp.fase1;

import java.io.IOException;
import java.net.*;

public class ServidorUDP {
    
    // Configuración
    private static final int PORT = 9876; 

    public static void main(String[] args) {
        
        DatagramSocket serverSocket = null;
        
        try {
            // 1. Crear el socket del servidor en el puerto especificado
            serverSocket = new DatagramSocket(PORT);
            System.out.println("Servidor UDP iniciado en el puerto " + PORT + ". Esperando paquetes...");

            byte[] receiveData = new byte[1024];
            
            while (true) {
                // Objeto para recibir el paquete del cliente
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                // Esperar a que llegue un paquete (esto bloquea el hilo)
                serverSocket.receive(receivePacket);

                // Obtener datos y detalles del cliente que envió el paquete
                String tramaRecibida = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress IPAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                
                System.out.println("\n--- PAQUETE RECIBIDO de " + IPAddress.getHostAddress() + ":" + clientPort + " ---");
                
                // 2. *** APLICACIÓN DEL REQUISITO CLAVE: VALIDACIÓN ROBUSTA ***
                
                if (tramaRecibida.startsWith("@") && tramaRecibida.endsWith("@")) {
                    // La trama tiene los delimitadores de inicio y fin
                    String contenido = tramaRecibida.substring(1, tramaRecibida.length() - 1);
                    
                    if (contenido.contains("#")) {
                        // La trama contiene el separador de comando/dato
                        
                        String[] partes = contenido.split("#", 2); 
                        String comando = partes[0];
                        String nombre = partes[1]; // El dato

                        if ("hola".equals(comando)) {
                            
                            // *** TRAMA CORRECTA: Procesar y Responder ***
                            System.out.println("Trama OK! Cliente: " + nombre);
                            
                            // Preparar respuesta
                            String respuesta = "Hola, " + nombre + ". Confirmacion de recepción.";
                            byte[] sendData = respuesta.getBytes();

                            // 3. Crear y enviar la respuesta al cliente
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, clientPort);
                            serverSocket.send(sendPacket);
                            System.out.println("Respuesta enviada: " + respuesta);
                            
                        } else {
                            // Comando no reconocido
                            System.out.println("Trama incorrecta. Comando desconocido: " + comando + ". Ignorando...");
                        }
                    } else {
                        // Falta el '#'
                        System.out.println("Trama incorrecta. Falta separador '#'. Ignorando...");
                    }
                } else {
                    // Faltan las arrobas
                    System.out.println("Trama incorrecta. Faltan delimitadores '@'. Ignorando: " + tramaRecibida);
                }
                
                // Si la trama es incorrecta, el servidor simplemente vuelve a 'while(true)' sin hacer nada más.
            }
        } catch (SocketException e) {
            System.err.println("Error de Socket: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error de I/O: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
                System.out.println("Servidor apagado.");
            }
        }
    }
}
