/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ejercicioudp.fase2;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ChatPeer {
    
    private static final int PORT = 9876; 
    private static final String BROADCAST_IP = "172.16.8.255";

    private static List<InetSocketAddress> amigos = new ArrayList<>();
    private static InetAddress miIP; // Para filtrar auto-respuestas

    public static void main(String[] args) {
        
        try {
            // Obtener mi propia IP para el filtrado (Fase 2)
            miIP = InetAddress.getLocalHost(); 
            System.out.println("Mi IP: " + miIP.getHostAddress());
            
            // Iniciar el hilo de escucha continua
            Thread listenerThread = new Thread(new ListenerTask());
            listenerThread.start();
            
            // Iniciar el cliente/enviador (para enviar el broadcast)
            iniciarChatClient(listenerThread);
            
        } catch (UnknownHostException e) {
            System.err.println("Error al obtener la IP local: " + e.getMessage());
        }
    }
    
    // --- TAREA DE ESCUCHA (SERVIDOR) ---
    static class ListenerTask implements Runnable {
        private DatagramSocket serverSocket;
        
        @Override
        public void run() {
            try {
                serverSocket = new DatagramSocket(PORT);
                System.out.println("Servidor/Peer iniciado en el puerto " + PORT + ". Escuchando...");

                byte[] receiveData = new byte[1024];
                
                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);

                    String tramaRecibida = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    InetAddress senderIP = receivePacket.getAddress();
                    int senderPort = receivePacket.getPort();
                    
                    // 1. Filtrado (Fase 2): Ignorar paquetes si vienen de mí mismo
                    if (senderIP.equals(miIP)) {
                        continue; 
                    }
                    
                    procesarTrama(tramaRecibida, senderIP, senderPort, serverSocket);
                }
            } catch (SocketException e) {
                System.err.println("Listener Socket cerrado.");
            } catch (IOException e) {
                System.err.println("Error de I/O en Listener: " + e.getMessage());
            } finally {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            }
        }
        
        // --- PROCESAMIENTO Y VALIDACIÓN ROBUSTA (Fase 1) ---
        private void procesarTrama(String trama, InetAddress ip, int port, DatagramSocket socket) throws IOException {
            if (trama.startsWith("@") && trama.endsWith("@")) {
                String contenido = trama.substring(1, trama.length() - 1);
                
                if (contenido.contains("#")) {
                    String[] partes = contenido.split("#", 2); 
                    String comando = partes[0].toLowerCase();
                    String dato = partes[1];

                    InetSocketAddress senderAddress = new InetSocketAddress(ip, port);

                    switch (comando) {
                        case "hola":
                            // Fase 1: Saludo
                            System.out.println("\n[SALA]: Trama Saludo OK! Cliente: " + dato);
                            
                            // Responder al cliente (Fase 1)
                            String respuesta = "@ok#Bienvenido@";
                            enviarRespuesta(socket, respuesta, ip, port);
                            break;
                            
                        case "descub":
                            // Fase 2: Respuesta al Broadcast de descubrimiento
                            System.out.println("\n[SALA]: Recibido Broadcast de Descubrimiento de: " + dato);
                            
                            // Agregar a la lista de amigos (si no está ya)
                            if (!amigos.contains(senderAddress)) {
                                amigos.add(senderAddress);
                                System.out.println("--> Nuevo amigo agregado: " + ip.getHostAddress());
                            }
                            
                            // Responder directamente al remitente del broadcast
                            String respDescub = "@amigo#mi_nombre_de_chat@"; // El servidor también se presenta
                            enviarRespuesta(socket, respDescub, ip, port);
                            break;
                            
                        case "amigo":
                            // Recibida una respuesta al broadcast (Fase 2)
                            if (!amigos.contains(senderAddress)) {
                                amigos.add(senderAddress);
                                System.out.println("\n[DISCOVERY]: Encontrado nuevo par: " + dato + " (" + ip.getHostAddress() + ")");
                                System.out.println("Lista de Amigos actual: " + amigos.size());
                            }
                            break;
                            
                        default:
                            System.out.println("[ERROR]: Comando desconocido: " + comando + ". Ignorando...");
                    }
                } else {
                    System.out.println("[ERROR]: Trama incorrecta (falta #). Ignorando: " + trama);
                }
            } else {
                // Trama basura (ej. HOLA, 123)
                System.out.println("[ERROR]: Trama incorrecta (falta @). Ignorando: " + trama);
            }
        }
        
        private void enviarRespuesta(DatagramSocket socket, String mensaje, InetAddress ip, int port) throws IOException {
            byte[] sendData = mensaje.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
            socket.send(sendPacket);
            System.out.println("Enviada respuesta: " + mensaje + " a " + ip.getHostAddress());
        }
    }
    
    // --- TAREA DE ENVÍO (CLIENTE) ---
    private static void iniciarChatClient(Thread listenerThread) {
        // Lógica para enviar mensajes y Broadcasts
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            // Es bueno poner timeout aquí también para las pruebas de fase 1,
            // pero para el broadcast no es estrictamente necesario, ya que solo es un envío.
            // Para la Fase 1, el cliente debe esperar la respuesta:
            
            // clientSocket.setSoTimeout(5000); // Se usó en el ejemplo anterior si se quiere testear F1

            System.out.println("\n--- MODO CHAT/CLIENTE ---");
            
            // *** FASE 2: BROADCAST DE DESCUBRIMIENTO ***
            System.out.println("\n[DISCOVERY]: Enviando Broadcast...");
            String broadcastMsg = "@descub#mi_nombre_de_chat@"; // Usamos 'descub' como comando
            byte[] sendData = broadcastMsg.getBytes();
            
            // La IP de broadcast debe ser la última de tu segmento de red (ej. 172.16.8.255)
            InetAddress broadcastIP = InetAddress.getByName(BROADCAST_IP);
            
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastIP, PORT);
            clientSocket.send(sendPacket);
            System.out.println("Broadcast de descubrimiento enviado a " + BROADCAST_IP + ":" + PORT);

            // Se esperan unos segundos para que lleguen las respuestas de 'amigo' (Fase 2)
            Thread.sleep(2000); 
            
            // Aquí puedes imprimir la lista de amigos descubiertos
            System.out.println("\n*** Listado de Amigos Encontrados (" + amigos.size() + ") ***");
            for (InetSocketAddress friend : amigos) {
                System.out.println(" -> " + friend.getAddress().getHostAddress() + ":" + friend.getPort());
            }
            
            // ... Aquí iría el bucle para enviar mensajes de chat reales ...
            
        } catch (IOException e) {
            System.err.println("Error de I/O en Cliente: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
