import agents.*;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Collection;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Agente principal del juego
 */

public class MainAgent extends Agent {

    private GUI gui;
    private AID[] playerAgents;
    private GameParametersStruct parameters = new GameParametersStruct();

    
    ArrayList<PlayerInformation> players = new ArrayList<>();

    private boolean pausa = false;

    @Override
    protected void setup() {
        gui = new GUI(this);
        System.setOut(new PrintStream(gui.getLoggingOutputStream()));

        updatePlayers();
        gui.logLine("Agent " + getAID().getName() + " is ready.");
    }

    public int getN(){
        return parameters.N;
    }

    public void pausa(){
        pausa = true;
    }

    public void continuar(){
        pausa = false;
    }

    /**
     * Funcion que actualiza a los agentes
     * @return
     */
    public int updatePlayers() {

        players = new ArrayList<>();

        gui.logLine("Updating player list");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                gui.logLine("Found " + result.length + " players");
            }
            playerAgents = new AID[result.length];
            for (int i = 0; i < result.length; ++i) {
                playerAgents[i] = result[i].getName();
            }
        } catch (FIPAException fe) {
            gui.logLine(fe.getMessage());
        }

        int fila = 0;
        for (AID a : playerAgents) {
            String[] nombre = a.getLocalName().split("r");
            players.add(new PlayerInformation(a, Integer.parseInt(nombre[1]), fila++));
        }

        for (int i = 0; i < players.size(); i++) {
            players.get(i).partidasJugadas = 0;
            players.get(i).setPuntosTotales(0);
        }


        //Provisional
        String[] playerNames = new String[playerAgents.length];
        for (int i = 0; i < playerAgents.length; i++) {
            playerNames[i] = playerAgents[i].getName();
        }
        gui.setPlayersTopPanel(playerNames);
        //gui.setPlayersUI(playerNames);
        gui.actualizarLabelJugadores();
        gui.resetTabla();
        return 0;
    }

    /************************ ELIMINAR JUGADOR ************************/ 

    public void removePlayer(String nombreJugadorEliminado){
        
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);

            for(int i = 0; i < result.length; i++) {
                if(result[i].getName().equals(nombreJugadorEliminado)){

                    gui.logLine("Jugador a eliminar con nombre: " + nombreJugadorEliminado + " encontrado y eliminado");
                    //players.remove(i);
                    
                    playerAgents = new AID[result.length-1];
                    
                    
                    int x = 0;
                    for(int j = 0; j < result.length; j++){
                        if(i != j){
                            playerAgents[x] = result[j].getName();
                            x++;

                        }
                    }
                    
                    String[] playerNames = new String[playerAgents.length];
                    
                    for (int k = 0; k < playerAgents.length; k++) {
                        playerNames[k] = playerAgents[k].getName();
                    }
                    
                    //gui.setPlayersUI(playerNames);
                    gui.actualizarLabelJugadores();
                    break;

                }

            }
            
        } catch (FIPAException fe) {
            gui.logLine(fe.getMessage());
        }

    }
    
    public void enseñarPuntuacion(){

    }
    
    public int newGame() {
        addBehaviour(new GameManager());
        return 0;
    }

    /**
     * In this behavior this agent manages the course of a match during all
     * game.
     */
    private class GameManager extends SimpleBehaviour {

        @Override
        public void action() {
            //Assign the IDs
            //ArrayList<PlayerInformation> players = new ArrayList<>();
            
            /*int fila = 0;
            for (AID a : playerAgents) {
                String [] nombre = a.getLocalName().split("r");
                players.add(new PlayerInformation(a, Integer.parseInt(nombre[1]), fila++));
            }*/

            //Initialize (inform ID)
            for (PlayerInformation player : players) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Id#" + player.id + "#" + parameters.N + "," + parameters.E + "," + parameters.R + "," + parameters.I + "," + parameters.P);
                msg.addReceiver(player.aid);
                send(msg);
            }


            for(int i=0; i<playerAgents.length; i++){
                if(players.size()>5){   /* Si el ArrayList de players tiene mas de 5 jugadores sigue jugando el BattleRoyale */
                    playGame(players);
    
                }else{
                    System.out.println("Estamos en la FINAL!!!");   /* Si hay 5 jugadores en el ArrayList de players se juega la final del torneo */
                    playGameFinal(players);
                    break;
                }
            }
 
        }



        /**
         * Funcion que juega el BattleRoyale
         * @param players ArrayList de los jugadores
         */
        
        private void playGame(ArrayList<PlayerInformation> players) {
            
            int T = ((parameters.getN()*parameters.getE())/2); /* Threshold */
            
            Random rand = new Random();
            
            gui.setResetColumnaPartidas(); /* Reseteamos las columnas de partidas de la GUI */
            
            int minNumPartida=0;

            /*for(int i=0; i<players.size(); i++){
                players.get(i).partidasJugadas=0;
                players.get(i).setPuntosTotales(0);
            }*/
            
            for(int g=0; g<parameters.G; g++){

                //Collections.shuffle(players);

                
                gui.actualizarNumGeneracion(g); /* Actualiza la generacion en la que estamos en la GUI */

                ArrayList<PlayerInformation> playersRound = new ArrayList<>();
                int l=0;
                
                /***************************************** Generamos el array de jugadores que van a jugar una generacion *********************************/
                for(int s=0; s<players.size(); s++){
                    
                    if(players.get(s).getPartidasJugadas() < minNumPartida){
                        players.get(s).setPuedoJugar(true);
                    }
                    
                    if(players.get(s).getPuedoJugar() == true){
                        players.get(s).setFila(l);
                        playersRound.add(players.get(s));
                        l++;
                    }
                    
                    if(playersRound.size()==5) break;
                    
                }
                
                for(int s=0; s<players.size(); s++){
                    if(playersRound.size()==5) break;
                    
                    if(players.get(s).getPartidasJugadas() == minNumPartida){
                        players.get(s).setPuedoJugar(true);
                    }
                    
                    if(players.get(s).getPuedoJugar() == true){
                        players.get(s).setFila(l);
                        playersRound.add(players.get(s));
                        l++;
                    }
                    
                    
                }

                for(int s=0; s<players.size(); s++){
                    if(playersRound.size()==5) break;
                    
                    if(players.get(s).getPartidasJugadas() > minNumPartida){
                        players.get(s).setPuedoJugar(true);
                    }
                    
                    if(players.get(s).getPuedoJugar() == true){
                        players.get(s).setFila(l);
                        playersRound.add(players.get(s));
                        l++;
                    }
                    
                    
                }
                /************************************ Actualizamos el minimo de partidas que se han jugado ****************************************/
                
                for(int s=0; s<playersRound.size(); s++){
                    if(playersRound.get(s).getPartidasJugadas() <= minNumPartida){
                        minNumPartida=playersRound.get(s).getPartidasJugadas();
                    }
                }
                
                
                String[] playerNames = new String[playersRound.size()];
                for (int i = 0; i < playersRound.size(); i++) {
                    playerNames[i] = playersRound.get(i).aid.getName();
                }
                gui.setPlayersUI(playerNames);
                
                
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                
                for(int i=0; i < playersRound.size(); i++){
                    msg.addReceiver(playersRound.get(i).aid);
                }



                /********************************Cantidad de puntos con los que empieza cada generacion un jugador *****************************/
                int puntos[] = new int[playersRound.size()];

                for(int i=0; i<playersRound.size(); i++){
                    puntos[i] = parameters.getE();
                }
                
                for(int k=0; k<parameters.P; k++){

                    String mensajeNuevoJuego = "NewGame#";
                    for(int i=0; i<playersRound.size(); i++){
                        mensajeNuevoJuego = mensajeNuevoJuego + playersRound.get(i).id + ",";
                    }

                    mensajeNuevoJuego = mensajeNuevoJuego.replaceFirst(".$", "");

                    msg.setContent(mensajeNuevoJuego);
                    send(msg);

                    int pos[] = {0,0,0,0,0};

                    int puntosTotalesJuego = 0;
            


                    /******************* Calculamos la probabilidad de catastrofe para despues comprobar con el parameters.I ********************/
                    float probabilidadCatastrofe = (float) (0+ ( 1 - 0 ) * rand.nextFloat());

                    for (int i=0; i<parameters.R; i++) { 
                        gui.actualizarNumRondas(i,k);

                        while(pausa){
                            try{
                                Thread.sleep(50);
                            } catch(Exception e){
                                e.printStackTrace();
                            }

                        }

                        /***************************EMPIEZAN A ENVIARSE LOS MOVIMIENTOS Y A REALIZAR JUGADAS***************************/
                        for(int y=0; y<parameters.getN(); y++){

                            msg = new ACLMessage(ACLMessage.REQUEST);
                            msg.setContent("Action");
                            msg.addReceiver(playersRound.get(y).aid);
                            send(msg);

                            //gui.logLine("Main Waiting for movement");
                            ACLMessage move1 = blockingReceive();
                            //gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
                            pos[y] = Integer.parseInt(move1.getContent().split("#")[1]);
                            gui.setActualizarTabla(pos[y], playersRound.get(y).fila, i+1);

                        }
                        
                        /************************ Se informa a los jugadores con el mensaje Result# y los puntos gastados en la ronda por todos los jugadores *****************/
                        
                        msg = new ACLMessage(ACLMessage.INFORM);

                        for(int o=0; o<playersRound.size(); o++){
                            msg.addReceiver(playersRound.get(o).aid);
                        }

                        String resultado = "Results#";
                        for(int o=0; o<playersRound.size(); o++){
                            resultado = resultado + pos[o]+ ",";
                        }

                        resultado = resultado.replaceFirst(".$", "");

                        msg.setContent(resultado);
                        
                        for(int a=0; a<playersRound.size(); a++){
                            puntos[a] -= pos[a];
                            gui.setActualizarResultados(puntos[a], playersRound.get(a).fila);
                        }

                        send(msg);
                    }
                    


                    /************************ Calculamos los puntos totales de una partida por cada jugador para pasarlos mediante el mensaje GameOver# a cada uno *****************/

                    int gameOverPuntos[] = new int[playersRound.size()];
                    
                    for(int o=0; o<playersRound.size(); o++){
                        gameOverPuntos[o]= parameters.getE() - puntos[o];
                        puntosTotalesJuego = puntosTotalesJuego + gameOverPuntos[o];
                    }

                    /********************** Se comprueba si puede haber catastrofe por no haber llegado al Threshold ******************/
                    if(puntosTotalesJuego<T){

                        if(probabilidadCatastrofe<=parameters.getI()){ /******************* Si hay catastrofe se envia el mensaje pertinente******************/
                            //System.out.println("GameOver#--------------Catastrofe---------------------");

                            for(int i=0; i<puntos.length; i++){
                                puntos[i] = 0;
                            }

                            String gameOver = "GameOver#";
                            for(int o=0; o<playersRound.size(); o++){
                                gameOver = gameOver + "0,";
                            }
                            gameOver = gameOver.replaceFirst(".$", "");
                            

                            msg.setContent(gameOver);                  
                            send(msg);

                        }else if(probabilidadCatastrofe>parameters.getI()){ /************ Si no hay catastrofe aunque no se haya llegado al Threshold, se envia el mensaje ************/

                            String gameOver = "GameOver#";
                            for(int o=0; o<playersRound.size(); o++){
                                gameOver = gameOver+ puntos[o] + ",";
                            }
                            gameOver = gameOver.replaceFirst(".$", "");

                            msg.setContent(gameOver);

                            for(int i=0; i<playersRound.size(); i++){
                                int puntosSuma = playersRound.get(i).getPuntosTotales() + puntos[i];
                                playersRound.get(i).setPuntosTotales(puntosSuma);

                            }

                            /*****************Nos permite ver en el GUI quien esta apostando mas/menos puntos*************************/

                            if(puntos[0]>puntos[1] && puntos[0]>puntos[2] && puntos[1]>puntos[3] && puntos[0]>puntos[4]){
                                gui.setActualizarGanador(playersRound.get(0).fila);
                            }else if(puntos[1]>puntos[0] && puntos[1]>puntos[2] && puntos[1]>puntos[3] && puntos[1]>puntos[4]){
                                gui.setActualizarGanador(playersRound.get(1).fila);
                            }else if(puntos[2]>puntos[0] && puntos[2]>puntos[1] && puntos[2]>puntos[3] && puntos[2]>puntos[4]){
                                gui.setActualizarGanador(playersRound.get(2).fila); 
                            }else if(puntos[3]>puntos[0] && puntos[3]>puntos[1] && puntos[3]>puntos[2] && puntos[3]>puntos[4]){
                                gui.setActualizarGanador(playersRound.get(3).fila);   
                            }else if(puntos[4]>puntos[0] && puntos[4]>puntos[1] && puntos[4]>puntos[2] && puntos[4]>puntos[3]){
                                gui.setActualizarGanador(playersRound.get(4).fila);     
                            }

                            send(msg);
                        }
                        
                    }else{  /*****************Si se pasa el Threshold, se envian los resultados con normalidad ********************/
                        String gameOver = "GameOver#";
                        for(int o=0; o<playersRound.size(); o++){
                            gameOver = gameOver+ puntos[o] + ",";
                        }
                        gameOver = gameOver.replaceFirst(".$", "");

                        msg.setContent(gameOver);


                        for(int i=0; i<playersRound.size(); i++){
                            int puntosSuma = playersRound.get(i).getPuntosTotales() + puntos[i];
                            playersRound.get(i).setPuntosTotales(puntosSuma);
                        }

                        /****************** Nos permite ver en el GUI quien esta apostando mas/menos puntos *************************/

                        if(puntos[0]>puntos[1] && puntos[0]>puntos[2] && puntos[1]>puntos[3] && puntos[0]>puntos[4]){
                            gui.setActualizarGanador(playersRound.get(0).fila);
                        }else if(puntos[1]>puntos[0] && puntos[1]>puntos[2] && puntos[1]>puntos[3] && puntos[1]>puntos[4]){
                            gui.setActualizarGanador(playersRound.get(1).fila);
                        }else if(puntos[2]>puntos[0] && puntos[2]>puntos[1] && puntos[2]>puntos[3] && puntos[2]>puntos[4]){
                            gui.setActualizarGanador(playersRound.get(2).fila); 
                        }else if(puntos[3]>puntos[0] && puntos[3]>puntos[1] && puntos[3]>puntos[2] && puntos[3]>puntos[4]){
                            gui.setActualizarGanador(playersRound.get(3).fila);   
                        }else if(puntos[4]>puntos[0] && puntos[4]>puntos[1] && puntos[4]>puntos[2] && puntos[4]>puntos[3]){
                            gui.setActualizarGanador(playersRound.get(4).fila);     
                        }

                        
                        send(msg);
                    }

                    /********** Se resetean los puntos para gastar en una nueva partida ************/
                    
                    for(int e=0; e<playersRound.size(); e++){
                        puntos[e] = parameters.getE();   
                        
                    }

                    /************* Actualizamos las aportaciones de cada jugador en la GUI **************/
                    for(int i=0; i<playersRound.size(); i++){
                        gui.setActualizarAportacion(playersRound.get(i).getPuntosTotales(), playersRound.get(i).fila);
                    }
                    
                /************* Pausa para jugar de partida en partida(depuracion) **************/
                //pausa=true;

                }
                
                /**************For que permite actualizar el numero de partidas jugadas de cada jugador y actualizar un booleano de control ****************/
                for(int e=0; e<playersRound.size(); e++){
                    playersRound.get(e).partidasJugadas++;
                    playersRound.get(e).setPuedoJugar(false);
                }

                minNumPartida++;

                
            }
            
            /****************** Se comprueba quien es el jugador con menos puntuacion de entre todos los participantes y se procede a su eliminacion tras completar las 250 generaciones **********/
            int jugadorMenosPuntos = 1000000000;
            for(int i=0; i<players.size(); i++){
                if(players.get(i).getPuntosTotales() < jugadorMenosPuntos){
                    jugadorMenosPuntos = players.get(i).getPuntosTotales();

                }
            }

            for(int i=0; i<players.size(); i++){
                if(players.size() == 5){
                    System.out.println("Ultima ronda");

                } else if(players.get(i).getPuntosTotales() == jugadorMenosPuntos){
                    System.out.println("ELiminado :" + players.get(i).id);
                    players.remove(i);
                    System.out.println("Jugadores restantes : " + players.size());
                }
            }

        }


        /**
         * Funcion que juega la final del torneo
         * @param players ArrayList de los jugadores
         */
        private void playGameFinal(ArrayList<PlayerInformation> players) {
            
            int T = ((parameters.getN()*parameters.getE())/2); /* Threshold */
            
            Random rand = new Random();
            
            gui.setResetColumnaPartidas(); /* Reseteamos las columnas de partidas de la GUI */

            ArrayList<PlayerInformation> playersRound = new ArrayList<>();

            int l=0;
            
            /****************************************** Generamos el array de jugadores que van a jugar la final *********************************/
            for(int s=0; s<players.size(); s++){
                players.get(s).setPuntosTotales(0);
                players.get(s).setFila(l);
                playersRound.add(players.get(s));
                l++;
                
            }
            
            String[] playerNames = new String[playersRound.size()];
            for (int i = 0; i < playersRound.size(); i++) {
                playerNames[i] = playersRound.get(i).aid.getName();
            }
            gui.setPlayersUI(playerNames);
            
            
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            
            for(int i=0; i < playersRound.size(); i++){
                msg.addReceiver(playersRound.get(i).aid);
            }

            /********************************* Cantidad de puntos con los que empieza cada generacion un jugador *****************************/
            int puntos[] = new int[playersRound.size()];

            for(int i=0; i<playersRound.size(); i++){
                puntos[i] = parameters.getE();
            }

            /******************************* Comienzan las partidas de la final ******************************/
                
            for(int k=0; k<parameters.Final; k++){

                String mensajeNuevoJuego = "NewGame#";
                for(int i=0; i<playersRound.size(); i++){
                    mensajeNuevoJuego = mensajeNuevoJuego + playersRound.get(i).id + ",";
                }

                mensajeNuevoJuego = mensajeNuevoJuego.replaceFirst(".$", "");

                msg.setContent(mensajeNuevoJuego);
                send(msg);

                int pos[] = {0,0,0,0,0};

                int puntosTotalesJuego = 0;
        
                /******************** Calculamos la probabilidad de catastrofe para despues comprobar con el parameters.I ********************/
                float probabilidadCatastrofe = (float) (0+ ( 1 - 0 ) * rand.nextFloat());

                for (int i=0; i<parameters.R; i++) { 
                    gui.actualizarNumRondas(i,k);

                    while(pausa){
                        try{
                            Thread.sleep(50);
                        } catch(Exception e){
                            e.printStackTrace();
                        }

                    }

                    /***************************EMPIEZAN A ENVIARSE LOS MOVIMIENTOS Y A REALIZAR JUGADAS***************************/
                    for(int y=0; y<parameters.getN(); y++){

                        msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.setContent("Action");
                        msg.addReceiver(playersRound.get(y).aid);
                        send(msg);

                        //gui.logLine("Main Waiting for movement");
                        ACLMessage move1 = blockingReceive();
                        //gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
                        pos[y] = Integer.parseInt(move1.getContent().split("#")[1]);
                        gui.setActualizarTabla(pos[y], playersRound.get(y).fila, i+1);

                    }
                    
                    /************************* Se informa a los jugadores con el mensaje Result# y los puntos gastados en la ronda por todos los jugadores *****************/
                    
                    msg = new ACLMessage(ACLMessage.INFORM);

                    for(int o=0; o<playersRound.size(); o++){
                        msg.addReceiver(playersRound.get(o).aid);
                    }

                    String resultado = "Results#";
                    for(int o=0; o<playersRound.size(); o++){
                        resultado = resultado + pos[o]+ ",";
                    }

                    resultado = resultado.replaceFirst(".$", "");

                    msg.setContent(resultado);
                    
                    for(int a=0; a<playersRound.size(); a++){
                        puntos[a] -= pos[a];
                        gui.setActualizarResultados(puntos[a], playersRound.get(a).fila);
                    }

                    send(msg);
                }

                /************************ Calculamos los puntos totales de una partida por cada jugador para pasarlos  mediante el mensaje GameOver# a cada uno *****************/

                int gameOverPuntos[] = new int[playersRound.size()];
                
                for(int o=0; o<playersRound.size(); o++){
                    gameOverPuntos[o]= parameters.getE() - puntos[o];
                    puntosTotalesJuego = puntosTotalesJuego + gameOverPuntos[o];
                }



                if(puntosTotalesJuego<T){   /*********************** Se comprueba si puede haber catastrofe por no haber llegado al Threshold ******************/

                    if(probabilidadCatastrofe<=parameters.getI()){  /******************* Si hay catastrofe se envia el mensaje pertinente ******************/
                        System.out.println("GameOver#--------------Catastrofe---------------------");

                        for(int i=0; i<puntos.length; i++){
                            puntos[i] = 0;
                        }

                        String gameOver = "GameOver#";
                        for(int o=0; o<playersRound.size(); o++){
                            gameOver = gameOver + "0,";
                        }
                        gameOver = gameOver.replaceFirst(".$", "");
                        

                        msg.setContent(gameOver);                  
                        send(msg);

                    }else if(probabilidadCatastrofe>parameters.getI()){ /************ Si no hay catastrofe aunque no se haya llegado al Threshold, se envia el mensaje ************/

                        String gameOver = "GameOver#";
                        for(int o=0; o<playersRound.size(); o++){
                            gameOver = gameOver+ puntos[o] + ",";
                        }
                        gameOver = gameOver.replaceFirst(".$", "");


                        msg.setContent(gameOver);

                        for(int i=0; i<playersRound.size(); i++){
                            int puntosSuma = playersRound.get(i).getPuntosTotales() + puntos[i];
                            playersRound.get(i).setPuntosTotales(puntosSuma);

                        }


                        /***************** Nos permite ver en el GUI quien esta apostando mas/menos puntos *************************/

                        if(puntos[0]>puntos[1] && puntos[0]>puntos[2] && puntos[1]>puntos[3] && puntos[0]>puntos[4]){
                            gui.setActualizarGanador(playersRound.get(0).fila);
                        }else if(puntos[1]>puntos[0] && puntos[1]>puntos[2] && puntos[1]>puntos[3] && puntos[1]>puntos[4]){
                            gui.setActualizarGanador(playersRound.get(1).fila);
                        }else if(puntos[2]>puntos[0] && puntos[2]>puntos[1] && puntos[2]>puntos[3] && puntos[2]>puntos[4]){
                            gui.setActualizarGanador(playersRound.get(2).fila); 
                        }else if(puntos[3]>puntos[0] && puntos[3]>puntos[1] && puntos[3]>puntos[2] && puntos[3]>puntos[4]){
                            gui.setActualizarGanador(playersRound.get(3).fila);   
                        }else if(puntos[4]>puntos[0] && puntos[4]>puntos[1] && puntos[4]>puntos[2] && puntos[4]>puntos[3]){
                            gui.setActualizarGanador(playersRound.get(4).fila);     
                        }

                        send(msg);
                    }
         
                }else{  /***************** Si se pasa el Threshold, se envian los resultados con normalidad ********************/
                    String gameOver = "GameOver#";
                    for(int o=0; o<playersRound.size(); o++){
                        gameOver = gameOver+ puntos[o] + ",";
                    }
                    gameOver = gameOver.replaceFirst(".$", "");


                    msg.setContent(gameOver);


                    for(int i=0; i<playersRound.size(); i++){
                        int puntosSuma = playersRound.get(i).getPuntosTotales() + puntos[i];
                        playersRound.get(i).setPuntosTotales(puntosSuma);
                    }

                    /****************** Nos permite ver en el GUI quien esta apostando mas/menos puntos *************************/
                    
                    if(puntos[0]>puntos[1] && puntos[0]>puntos[2] && puntos[1]>puntos[3] && puntos[0]>puntos[4]){
                        gui.setActualizarGanador(playersRound.get(0).fila);
                    }else if(puntos[1]>puntos[0] && puntos[1]>puntos[2] && puntos[1]>puntos[3] && puntos[1]>puntos[4]){
                        gui.setActualizarGanador(playersRound.get(1).fila);
                    }else if(puntos[2]>puntos[0] && puntos[2]>puntos[1] && puntos[2]>puntos[3] && puntos[2]>puntos[4]){
                        gui.setActualizarGanador(playersRound.get(2).fila); 
                    }else if(puntos[3]>puntos[0] && puntos[3]>puntos[1] && puntos[3]>puntos[2] && puntos[3]>puntos[4]){
                        gui.setActualizarGanador(playersRound.get(3).fila);   
                    }else if(puntos[4]>puntos[0] && puntos[4]>puntos[1] && puntos[4]>puntos[2] && puntos[4]>puntos[3]){
                        gui.setActualizarGanador(playersRound.get(4).fila);     
                    }

                    
                    send(msg);
                }
                
                /********** Se resetean los puntos para gastar en una nueva partida ************/

                for(int e=0; e<playersRound.size(); e++){
                    puntos[e] = parameters.getE();   
                    
                }

                /************* Actualizamos las aportaciones de cada jugador en la GUI **************/

                for(int i=0; i<playersRound.size(); i++){
                    gui.setActualizarAportacion(playersRound.get(i).getPuntosTotales(), playersRound.get(i).fila);
                }
                
                /************* Pausa para jugar de partida en partida(depuracion) **************/
                //pausa=true;

            }


            /*for(int e=0; e<playersRound.size(); e++){
                playersRound.get(e).partidasJugadas++;
                playersRound.get(e).setPuedoJugar(false);
            }*/

            /******** Se comprueban las posiciones finales y se establece el podium, que se le pasa a una funcion del GUI para que saque por pantalla los resultados finales ********/
            
            int primeraPosicion = 0, segundaPosicion = 0, terceraPosicion = 0;
            String ganador = "", segundo = "", tercero = "";

            for(int i=0; i<players.size(); i++){
                if(players.get(i).getPuntosTotales() > primeraPosicion){
                    primeraPosicion = players.get(i).getPuntosTotales();

                }
            }

            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getPuntosTotales() < primeraPosicion && players.get(i).getPuntosTotales() > segundaPosicion) {
                    segundaPosicion = players.get(i).getPuntosTotales();

                }
            }

            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getPuntosTotales() < segundaPosicion && players.get(i).getPuntosTotales() > terceraPosicion) {
                    terceraPosicion = players.get(i).getPuntosTotales();

                }
            }


            for(int i=0; i<players.size(); i++){
                if(players.get(i).getPuntosTotales() == primeraPosicion){
                    ganador = players.get(i).aid.getName();
                }
                
                if(players.get(i).getPuntosTotales() == segundaPosicion){
                    segundo = players.get(i).aid.getName();
                }

                if(players.get(i).getPuntosTotales() == terceraPosicion){
                    tercero = players.get(i).aid.getName();
                }
            }


            gui.ganador(ganador, segundo, tercero);

        }
        
        @Override
        public boolean done() {
            return true;
        }
    }

    public class PlayerInformation {

        AID aid;                /* AID del jugador */
        int id;                 /* Id del jugador */
        int fila;               /* Fila de la tabla en la que se encuentra el jugador en una generacion concreta */
        int partidasJugadas=0;  /* Variable que cuenta el numero de partidas que ha jugado un jugador concreto */
        int puntosTotales=0;    /* Puntos totales acumulados por un jugador */
        boolean puedoJugar;     /* Booleano que nos permite ver si este jugador en cuestion puede jugar o no dependiendo de su numero de partidas jugadas */

        public PlayerInformation(AID a, int i, int f) {
            aid = a;
            id = i;
            fila = f;
        }

        public int getPartidasJugadas(){
            return partidasJugadas;
        }


        public int getPuntosTotales(){
            return puntosTotales;
        }

        public void setPuntosTotales(int puntosTotales){
            this.puntosTotales = puntosTotales;
        }

        public void setFila(int fila){
            this.fila = fila;
        }

        public boolean getPuedoJugar(){
            return puedoJugar;
        }

        public void setPuedoJugar(boolean jugar){
            puedoJugar = jugar;
        }

        @Override
        public boolean equals(Object o) {
            return aid.equals(o);
        }

    }

    public class GameParametersStruct {

        int N;      /* Num de jugadores por partida */
        int S;      /* Num de puntos que puede jugar como maximo un jugador */
        int E;      /* Puntos totales que tiene un jugador para poder gastar */
        int R;      /* Numero de rondas */
        float I;    /* Probabilidad de desastre */
        int P;      /* Numero de partidas que se juegan*/
        int G;      /* Numero de generaciones */
        int Final;  /* Numero de partidas que se juegan en la final */

        public GameParametersStruct() {
            N = 5;                          
            S = 4;                          
            E = 40;                        
            I = (float) 0.8;                  
            R = 10;                         
            P = 100;                         
            G = 250;                        
            Final = 1000;
        }

        public String getParametros(){
            return parameters.N+","+parameters.E+","+parameters.R+","+parameters.I+","+parameters.P+","+parameters.G;
        }

        public void setParametros(int N, int E, int R, float I, int P, int G){
            parameters.N = N;
            parameters.E = E;
            parameters.R = R;
            parameters.I = I;
            parameters.P = P;
            parameters.G = G;
        }

        public int getN(){
            return parameters.N;
        }

        public int getE(){
            return parameters.E;
        }

        public float getI(){
            return parameters.I;
        }

        public int getP(){
            return parameters.P;
        }

        public int getG(){
            return parameters.G;
        }

        public void setP(int P){
            parameters.P = P;
        }

    }
}
