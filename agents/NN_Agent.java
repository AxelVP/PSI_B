package agents;

import jade.content.onto.annotations.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Serializable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import javax.security.auth.x500.X500PrivateCredential;

public class NN_Agent extends Agent {

    private State state;
    private AID mainAgent;
    private int myId, opponentId, opponentId0, opponentId1, opponentId2, opponentId3, opponentId4, miPosicion, misPuntos;
    private int[] puntosOponentes = { 0, 0, 0, 0, 0 };
    private int N=0, E=0, R=0, P=0, puntosNecesariosDesastre=0;
    private float I;
    private ACLMessage msg;

    private int vectorJugadasPorPartida[] = { 3, 2, 1, 4, 4, 2, 1, 2, 3, 3 };

    private int[][] jugadasRealizadas = {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                                        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                                        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                                        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                                        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};

    int rondaActual, apuesta = 0;

    /*
     * Matriz de comprobación de mejor jugada en base a las jugadas posibles
     * Filas: Numero de puntos que podemos apostar (0,1,2,3,4))
     * Columnas: Numero de la ronda en una partida (1-10)
     */
    private double[][] matrizEleccion = { { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
                                        { 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2 },
                                        { 0.4, 0.4, 0.4, 0.4, 0.4, 0.4, 0.4, 0.4, 0.4, 0.4 },
                                        { 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6 },
                                        { 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8 },
                                        { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 } };


    private double[][] som = {{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
                            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
                            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
                            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
                            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 }};

                    

    boolean gameOver = false, ganador = false;
    private double dLearnRate = 1.0;

    protected void setup() {
        state = State.s0NoConfig;

        // Register in the yellow pages as a player
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        sd.setName("Game");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new Play());
        // System.out.println("RandomAgent " + getAID().getName() + " is ready.");

    }

    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        // System.out.println("RandomPlayer " + getAID().getName() + " terminating.");
    }

    enum State {
        s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
    }

    private class Play extends CyclicBehaviour {
        Random random = new Random();

        @Override
        public void action() {
            // System.out.println(getAID().getName() + ":" + state.name());
            msg = blockingReceive();
            if (msg != null) {
                // System.out.println(getAID().getName() + " received " + msg.getContent() + "
                // from " + msg.getSender().getName()); //DELETEME
                // -------- Agent logic
                switch (state) {
                    case s0NoConfig:
                        // If INFORM Id#_#_,_,_,_ PROCESS SETUP --> go to state 1
                        // Else ERROR
                        if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
                            boolean parametersUpdated = false;
                            inicializarMatriz();

                            try {
                                parametersUpdated = validateSetupMessage(msg);
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                            }
                            if (parametersUpdated)
                                state = State.s1AwaitingGame;

                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;

                    case s1AwaitingGame:

                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            if (msg.getContent().startsWith("Id#")) { // Game settings updated
                                try {
                                    validateSetupMessage(msg);
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                            } else if (msg.getContent().startsWith("NewGame#")) {
                                boolean gameStarted = false;
                                try {
                                    gameStarted = validateNewGame(msg.getContent());

                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                                if (gameStarted) {
                                    rondaActual = 0;
                                    state = State.s2Round;
                                }
                            }
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;

                    case s2Round:

                        if (msg.getPerformative() == ACLMessage.REQUEST /* && msg.getContent().startsWith("Position") */) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(mainAgent);

                            int jugada = elegirJugada(rondaActual, vectorJugadasPorPartida);    // Seleccionamos la mejor jugada posible
                            vectorJugadasPorPartida[rondaActual] = jugada;                      // Guardamos en el vector la apuesta hecha
                                                                                                // para ver al final si se refuerzan las
                                                                                                // jugadas o no
                            
                            msg.setContent("Action#" + jugada);
                            

                            // System.out.println(getAID().getName() + " sent " + msg.getContent());
                            send(msg);
                            state = State.s3AwaitingResult;

                        } else if (msg.getPerformative() == ACLMessage.INFORM
                                && msg.getContent().startsWith("Changed#")) {
                            // Process changed message, in this case nothing
                        } else if (msg.getPerformative() == ACLMessage.INFORM
                                && msg.getContent().startsWith("GameOver")) {

                            /*
                             * Comprobamos si debemos reforzar la jugada o no
                             */
                            gameOver = false;
                            ganador = false;

                            gameOver = comprobarGameOver(msg);

                            ganador = comprobarGanador(msg);


                            refuerzoJugada(vectorJugadasPorPartida, ganador, gameOver);


                            
                            /*BufferedWriter salidaTxt = null;
                            String nombreFichero = "matrizNeurona" + myId + ".txt";
                            try {
                                salidaTxt = new BufferedWriter(new FileWriter(nombreFichero));
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            float salida1 = 0;
                            String salida = null;

                            for (int i = 0; i < 5; i++) {
                                for (int j = 0; j < 10; j++) {
                                    try {
                                        salida1 = (float) som[i][j];
                                        salida = Float.toString(salida1) + " ";

                                        salidaTxt.write(salida);
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }

                                }
                                try {
                                    salidaTxt.write("\n");
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                            }
                            try {
                                salidaTxt.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }*/

                            state = State.s1AwaitingGame;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message:" + msg.getContent());
                        }
                        break;
                    case s3AwaitingResult:
                        // If INFORM RESULTS --> go to state 2
                        // Else error
                        if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {

                            resultados(msg.getContent(), rondaActual);
                            // Process results
                            rondaActual++;
                            state = State.s2Round;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;

                }
            }
        }

        /**
         * Validates and extracts the parameters from the setup message
         *
         * @param msg ACLMessage to process
         * @return true on success, false on failure
         */
        private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
            int tN, tE, tR, tP, tMyId;
            float tI;
            String msgContent = msg.getContent();

            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3)
                return false;
            if (!contentSplit[0].equals("Id"))
                return false;
            tMyId = Integer.parseInt(contentSplit[1]);

            String[] parametersSplit = contentSplit[2].split(",");
            if (parametersSplit.length != 5)
                return false;
            tN = Integer.parseInt(parametersSplit[0]);
            tE = Integer.parseInt(parametersSplit[1]);
            tR = Integer.parseInt(parametersSplit[2]);
            tI = Float.parseFloat(parametersSplit[3]);
            tP = Integer.parseInt(parametersSplit[4]);

            // At this point everything should be fine, updating class variables
            mainAgent = msg.getSender();
            N = tN;
            E = tE;
            R = tR;
            I = tI;
            P = tP;
            myId = tMyId;
            puntosNecesariosDesastre = (N*E)/2;
            return true;
        }

        /**
         * Processes the contents of the New Game message
         * 
         * @param msgContent Content of the message
         * @return true if the message is valid
         */
        public boolean validateNewGame(String msgContent) {
            int msgId0, msgId1, msgId2, msgId3, msgId4;

            int[] msgId = {};
            String[] contentSplit = msgContent.split("#");
            if (!contentSplit[0].equals("NewGame"))
                return false;
            String[] idSplit = contentSplit[1].split(",");

            msgId0 = Integer.parseInt(idSplit[0]);
            msgId1 = Integer.parseInt(idSplit[1]);
            msgId2 = Integer.parseInt(idSplit[2]);
            msgId3 = Integer.parseInt(idSplit[3]);
            msgId4 = Integer.parseInt(idSplit[4]);

            if (myId == msgId0) {
                miPosicion = 0;
                opponentId1 = 1;
                opponentId2 = 2;
                opponentId3 = 3;
                opponentId4 = 4;
                return true;

            } else if (myId == msgId1) {

                opponentId1 = 0;
                opponentId2 = 2;
                opponentId3 = 3;
                opponentId4 = 4;
                miPosicion = 1;
                return true;

            } else if (myId == msgId2) {

                opponentId1 = 0;
                opponentId2 = 1;
                opponentId3 = 3;
                opponentId4 = 4;
                miPosicion = 2;
                return true;

            } else if (myId == msgId3) {

                opponentId1 = 0;
                opponentId2 = 1;
                opponentId3 = 2;
                opponentId4 = 4;
                miPosicion = 3;
                return true;

            } else if (myId == msgId4) {

                opponentId1 = 0;
                opponentId2 = 1;
                opponentId3 = 2;
                opponentId4 = 3;
                miPosicion = 4;
                return true;
            }
            return false;
        }

        /**
         *  Funcion que guarda las jugadas realizadas por cada jugador en una matriz donde las columnas son la ronda en la que se encuentran y las filas cada uno de lo sjugadores de la partida
         * @param msgContent El contenido del mensaje enviado por el MainAgent
         * @param ronda La ronda actual dentro de la partida
         */
        public void resultados(String msgContent, int ronda) {

            int resultado0, resultado1, resultado2, resultado3, resultado4;

            String[] contentSplit = msgContent.split("#");
            if (!contentSplit[0].equals("Results"))
                System.out.println("Error en la funcion resultados");

            String[] idSplit = contentSplit[1].split(",");

            resultado0 = Integer.parseInt(idSplit[0]);
            resultado1 = Integer.parseInt(idSplit[1]);
            resultado2 = Integer.parseInt(idSplit[2]);
            resultado3 = Integer.parseInt(idSplit[3]);
            resultado4 = Integer.parseInt(idSplit[4]);

            
            jugadasRealizadas[0][ronda] = resultado0;
            jugadasRealizadas[1][ronda] = resultado1;
            jugadasRealizadas[2][ronda] = resultado2;
            jugadasRealizadas[3][ronda] = resultado3;
            jugadasRealizadas[4][ronda] = resultado4;

        }

        public void inicializarMatriz(){

            for(int i=0; i<5; i++){
                for(int j=0; j<10; j++){
                    double inicializarMatriz = Math.random();
                    som[i][j] = inicializarMatriz;
                }
            }
        }


        /**
         * Funcion para elegir la jugada que se va a hacer en la ronda
         * 
         * @param ronda La ronda actual dentro de la partida
         * @param puntosUsadosAnteriorPartida El vector de jugadas realizadas en la ronda anterior
         * @return Devuelve la fila de la jugada escogida teniendo en cuenta la ronda en la que estamos
         *  
         */
        public int elegirJugada(int ronda, int[] puntosUsadosAnteriorPartida) {

            int eleccion = 0;
            apuesta = 0;
            int x=0, y=0;
            double dNorm, dNormMin = Double.MAX_VALUE;

            for (int fila = 0; fila < 5; fila++) {
                dNorm = 0;
                for (int i = 0; i < puntosUsadosAnteriorPartida.length; i++) {
                    dNorm += (puntosUsadosAnteriorPartida[i] - som[fila][ronda]) * (puntosUsadosAnteriorPartida[i] - som[fila][ronda]);
                    
                    if (dNorm < dNormMin) {
                        dNormMin = dNorm;
                        eleccion = fila;
                        x=fila;
                        y=ronda;
                    }
                }
            }
            

            for (int fila = 0; fila < 5; fila++) {

                if (matrizEleccion[fila][ronda] < eleccion && eleccion < matrizEleccion[fila+1][ronda]) {

                    eleccion = apuesta;
                }
                apuesta++;
            }


            return eleccion;
        }


        

        /**
         * Funcion que comprueba si hubo catastrofe o no
         * 
         * @param msg Se pasa el mensaje recibido por el MainAgent
         * @return true si el juego no ha teminado en catastrofe
         * @return false si el juego ha terminado en catastrofe
         * 
         */
        private boolean comprobarGameOver(ACLMessage msg) {

            int contadorCero = 0;
            String msgContent = msg.getContent();
            String[] contentSplit = msgContent.split("#");
            if (!contentSplit[0].equals("GameOver"))
                return false;
            String[] puntuacion = contentSplit[1].split(",");

            for (int i = 0; i < 5; i++) {
                if (Integer.parseInt(puntuacion[i]) == 0) {
                    contadorCero++;
                }
            }

            if (contadorCero == 5) {
                return false;
            } else {
                return true;
            }
        }

        
        /**
         * Funcion que comprueba si eres el ganador de la partida
         * 
         * @param msg Se pasa el mensaje recibido por el MainAgent
         * @return true si eres el ganador
         * @return false si no has sido tu el ganador
         * 
         */
        private boolean comprobarGanador(ACLMessage msg) {

            int soyGanador = 0;
            String msgContent = msg.getContent();
            String[] contentSplit = msgContent.split("#");
            if (!contentSplit[0].equals("GameOver"))
                return false;
            String[] puntuacion = contentSplit[1].split(",");

            for (int i = 0; i < 5; i++) {
                if (i == miPosicion) {
                    misPuntos = Integer.parseInt(puntuacion[i]);
                }
                puntosOponentes[i] = Integer.parseInt(puntuacion[i]);

            }

            for (int i = 0; i < 5; i++) {
                if (Integer.parseInt(puntuacion[i]) > soyGanador) {
                    soyGanador = Integer.parseInt(puntuacion[i]);
                }
            }

            if (soyGanador == misPuntos) {
                return true;
            }

            return false;
        }


        /**
         * Funcion que refuerza la jugada si no hubo catastrofe o baja el peso si hubo desastre
         * 
         * @param vectorJugada El vector de jugadas a lo largo de la partida
         * @param ganador Si eres el jugador ganador de la partida
         * @param perdiste Si se perdio la partida por catastrofe
         * 
         */

        public void refuerzoJugada(int[] vectorJugada, boolean ganador, boolean perdiste) {

            double minimo= 0.001, refuerzoGanador = 0.4, refuerzoVecinoGanador = 0.3, refuerzoNoGanador = 0.3, refuerzoVecinoNoGanador = 0.2, MediaPorEncima = 0.6, MediaPorDebajo = 0.5;

            int mediaPuntuacion = 0;

            for (int i = 0; i < 5; i++) {
                mediaPuntuacion = mediaPuntuacion + puntosOponentes[i];
            }
            mediaPuntuacion = mediaPuntuacion / 4;

            
            for (int i = 0; i < vectorJugada.length; i++) {

                switch (vectorJugada[i]) {
                    case 0:

                        if (!perdiste) {

                            /*
                             * if(mediaPuntuacion>misPuntos){
                             * matrizEleccion[1][i] += refuerzoMediaPorEncima*(1-matrizEleccion[0][i]);
                             * 
                             * }else{
                             * matrizEleccion[1][i] -= matrizEleccion[1][i] * (1-refuerzoMediaPorDebajo);
                             * acumulado = matrizEleccion[1][i];
                             * }
                             */

                            if (ganador) {

                                matrizEleccion[1][i] += refuerzoGanador * (1.0 - matrizEleccion[1][i]);

                            } else {
                                matrizEleccion[1][i] += refuerzoNoGanador * (1.0 - matrizEleccion[1][i]);

                            }

                        } else {

                            if (mediaPuntuacion >= misPuntos) {
                                matrizEleccion[1][i] = matrizEleccion[1][i] * (1 - MediaPorDebajo);

                            } else if (mediaPuntuacion < misPuntos) {
                                matrizEleccion[1][i] = matrizEleccion[1][i] * (1.0 - MediaPorEncima);
                            }

                        }
                        break;

                    case 1:

                        if (!perdiste) {

                            /*
                             * if(mediaPuntuacion>misPuntos){
                             * matrizEleccion[2][i] += refuerzoMediaPorEncima*(1-matrizEleccion[0][i]);
                             * 
                             * }else{
                             * matrizEleccion[2][i] -= matrizEleccion[2][i] * (1-refuerzoMediaPorDebajo);
                             * acumulado = matrizEleccion[2][i];
                             * }
                             */

                            if (ganador) {

                                matrizEleccion[2][i] += refuerzoGanador * (1.0 - matrizEleccion[2][i]);

                            } else {
                                matrizEleccion[2][i] += refuerzoNoGanador * (1.0 - matrizEleccion[2][i]);

                            }

                        } else {

                            if (mediaPuntuacion >= misPuntos) {
                                matrizEleccion[2][i] = matrizEleccion[2][i] * (1.0 - MediaPorDebajo);

                            } else if (mediaPuntuacion < misPuntos) {
                                matrizEleccion[2][i] = matrizEleccion[2][i] * (1.0 - MediaPorEncima);
                            }

                        }
                        break;

                    case 2:

                        if (!perdiste) {

                            /*
                             * if(mediaPuntuacion>misPuntos){
                             * matrizEleccion[3][i] += refuerzoMediaPorEncima*(1-matrizEleccion[0][i]);
                             * 
                             * }else{
                             * matrizEleccion[3][i] -= matrizEleccion[3][i] * (1-refuerzoMediaPorDebajo);
                             * acumulado = matrizEleccion[3][i];
                             * }
                             */

                            if (ganador) {

                                matrizEleccion[3][i] += refuerzoGanador * (1.0 - matrizEleccion[3][i]);
                            } else {
                                matrizEleccion[3][i] += refuerzoNoGanador * (1.0 - matrizEleccion[3][i]);

                            }

                        } else {

                            if (mediaPuntuacion >= misPuntos) {
                                matrizEleccion[3][i] = matrizEleccion[3][i] * (1.0 - MediaPorDebajo);

                            } else if (mediaPuntuacion < misPuntos) {
                                matrizEleccion[3][i] = matrizEleccion[3][i] * (1.0 - MediaPorEncima);
                            }

                        }
                        break;

                    case 3:

                        if (!perdiste) {

                            /*
                             * if(mediaPuntuacion>misPuntos){
                             * matrizEleccion[4][i] += refuerzoMediaPorEncima*(1-matrizEleccion[0][i]);
                             * 
                             * }else{
                             * matrizEleccion[4][i] -= matrizEleccion[4][i] * (1-refuerzoMediaPorDebajo);
                             * acumulado = matrizEleccion[4][i];
                             * }
                             */

                            if (ganador) {

                                matrizEleccion[4][i] += refuerzoGanador * (1.0 - matrizEleccion[4][i]);

                            } else {
                                matrizEleccion[4][i] += refuerzoNoGanador * (1.0 - matrizEleccion[4][i]);

                            }

                        } else {

                            if (mediaPuntuacion >= misPuntos) {
                                matrizEleccion[4][i] = matrizEleccion[4][i] * (1.0 - MediaPorDebajo);

                            } else if (mediaPuntuacion < misPuntos) {
                                matrizEleccion[4][i] = matrizEleccion[4][i] * (1.0 - MediaPorEncima);
                            }

                        }
                        break;

                    case 4:

                        if (!perdiste) {

                            /*
                             * if(mediaPuntuacion>misPuntos){
                             * matrizEleccion[5][i] += refuerzoMediaPorEncima*(1-matrizEleccion[0][i]);
                             * 
                             * }else{
                             * matrizEleccion[5][i] -= matrizEleccion[5][i] * (1-refuerzoMediaPorDebajo);
                             * acumulado = matrizEleccion[5][i];
                             * }
                             */

                            if (ganador) {

                                matrizEleccion[5][i] += refuerzoGanador * (1.0 - matrizEleccion[5][i]);

                            } else {
                                matrizEleccion[5][i] += refuerzoNoGanador * (1.0 - matrizEleccion[5][i]);

                            }

                        } else {

                            if (mediaPuntuacion >= misPuntos) {
                                matrizEleccion[5][i] = matrizEleccion[5][i] * (1.0 - MediaPorDebajo);

                            } else if (mediaPuntuacion < misPuntos) {
                                matrizEleccion[5][i] = matrizEleccion[5][i] * (1.0 - MediaPorEncima);
                            }

                        }
                        break;

                    default:
                        break;

                }

                /********************* Normalizamos la matriz **************************/
                for (int o = 0; o < 6; o++) {
                    for (int j = 0; j < 10; j++) {
                        matrizEleccion[o][j] = matrizEleccion[o][j] / matrizEleccion[5][j];
                    }
                }


                matrizEleccion[vectorJugada[i]][i] += refuerzoGanador * (1.0 - matrizEleccion[vectorJugada[i]][i]);

                /* Reforzamos los vecinos de la acción */

                if (i == 0) {
                    if (vectorJugada[i] == 0) {
                        matrizEleccion[vectorJugada[i] + 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] + 1][i]);
                        matrizEleccion[vectorJugada[i]][i + 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i + 1]);

                    } else if (vectorJugada[i] == 4) {
                        matrizEleccion[vectorJugada[i] - 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] - 1][i]);
                        matrizEleccion[vectorJugada[i]][i + 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i + 1]);

                    } else {
                        matrizEleccion[vectorJugada[i] + 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] + 1][i]);
                        matrizEleccion[vectorJugada[i] - 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] - 1][i]);
                        matrizEleccion[vectorJugada[i]][i + 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i + 1]);
                    }

                } else if (i == 9) {
                    if (vectorJugada[i] == 0) {
                        matrizEleccion[vectorJugada[i] + 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] + 1][i]);
                        matrizEleccion[vectorJugada[i]][i - 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i - 1]);

                    } else if (vectorJugada[i] == 4) {
                        matrizEleccion[vectorJugada[i] - 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] - 1][i]);
                        matrizEleccion[vectorJugada[i]][i - 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i - 1]);

                    } else {
                        matrizEleccion[vectorJugada[i] + 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] + 1][i]);
                        matrizEleccion[vectorJugada[i] - 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] - 1][i]);
                        matrizEleccion[vectorJugada[i]][i - 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i - 1]);
                    }

                } else {
                    if (vectorJugada[i] == 0) {
                        matrizEleccion[vectorJugada[i] + 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] + 1][i]);
                        matrizEleccion[vectorJugada[i]][i + 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i + 1]);
                        matrizEleccion[vectorJugada[i]][i - 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i - 1]);

                    } else if (vectorJugada[i] == 4) {
                        matrizEleccion[vectorJugada[i] - 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] - 1][i]);
                        matrizEleccion[vectorJugada[i]][i + 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i + 1]);
                        matrizEleccion[vectorJugada[i]][i - 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i - 1]);

                    } else {
                        matrizEleccion[vectorJugada[i] + 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] + 1][i]);
                        matrizEleccion[vectorJugada[i] - 1][i] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i] - 1][i]);
                        matrizEleccion[vectorJugada[i]][i + 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i + 1]);
                        matrizEleccion[vectorJugada[i]][i - 1] += dLearnRate
                                * (1.0 - matrizEleccion[vectorJugada[i]][i - 1]);
                    }
                }
            }
            

            /*BufferedWriter salidaTxt = null;
            String nombreFichero = "matrizJugadas" + myId + ".txt";
            try {
                salidaTxt = new BufferedWriter(new FileWriter(nombreFichero));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            float salida1 = 0;
            String salida = null;

            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 10; j++) {
                    try {
                        salida1 = (float) jugadasRealizadas[i][j];
                        salida = Float.toString(salida1) + " ";

                        salidaTxt.write(salida);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
                try {
                    salidaTxt.write("\n");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            try {
                salidaTxt.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }*/


        }
    }
}