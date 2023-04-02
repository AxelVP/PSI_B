import javax.management.JMException;
import javax.swing.*;
import javax.swing.plaf.synth.SynthSplitPaneUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.PlainDocument;

import jade.core.AID;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Interfaz grafica del juego
 */

public final class GUI extends JFrame implements ActionListener {
    JLabel leftPanelRoundsLabel;
    JLabel leftPanelGenerationLabel;
    JLabel leftPanelExtraInformation;
    JLabel infoNumJugadores;
    JList<String> list;
    private MainAgent mainAgent;
    private JPanel rightPanel;
    private JTextArea rightPanelLoggingTextArea;
    private LoggingOutputStream loggingOutputStream;
    DefaultTableModel modelo = new DefaultTableModel();
    private JTable payoffTable;

    int numJugadoresPorRonda=0;

    public GUI() {
        initUI();
    }

    public GUI(MainAgent agent) {
        mainAgent = agent;
        initUI();
        loggingOutputStream = new LoggingOutputStream(rightPanelLoggingTextArea);
    }

    public void log(String s) {
        Runnable appendLine = () -> {
            rightPanelLoggingTextArea.append('[' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + s);
            rightPanelLoggingTextArea.setCaretPosition(rightPanelLoggingTextArea.getDocument().getLength());
        };
        SwingUtilities.invokeLater(appendLine);
    }

    public OutputStream getLoggingOutputStream() {
        return loggingOutputStream;
    }

    public void logLine(String s) {
        log(s + "\n");
    }

    public void setPlayersUI(String[] players) {
        int i = 1;
        for (String s : players) {
            String [] nombreJugador = s.split("@");
                
            //if(numJugadoresPorRonda!=5){
                payoffTable.setValueAt(nombreJugador[0], i, 0);
                payoffTable.setValueAt(0, i, 12);

            //}

            numJugadoresPorRonda++;
            i++;
        //}
        }
    }

    public void setPlayersTopPanel(String[] players) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String s : players) {
            listModel.addElement(s);
        }
        list.setModel(listModel);
    }



    public void initUI() {
        setTitle("GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(600, 400));
        setPreferredSize(new Dimension(1000, 600));
        setJMenuBar(createMainMenuBar());
        setContentPane(createMainContentPane());
        pack();
        setVisible(true);
    }

    private Container createMainContentPane() {
        JPanel pane = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridy = 0;
        gc.weightx = 0.5;
        gc.weighty = 0.5;

        //LEFT PANEL
        gc.gridx = 0;
        gc.weightx = 1;
        pane.add(createLeftPanel(), gc);

        //CENTRAL PANEL
        gc.gridx = 1;
        gc.weightx = 8;
        pane.add(createCentralPanel(), gc);

        //RIGHT PANEL
        gc.gridx = 2;
        gc.weightx = 8;
        pane.add(createRightPanel(), gc);
        return pane;
    }

    

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        
        leftPanelRoundsLabel = new JLabel("Round 0 / null");
        leftPanelGenerationLabel = new JLabel("Generation: 0");
        JButton leftPanelNewButton = new JButton("New");
        leftPanelNewButton.addActionListener(actionEvent -> mainAgent.newGame());
        JButton leftPanelStopButton = new JButton("Stop");
        leftPanelStopButton.addActionListener(actionEvent -> mainAgent.pausa());
        JButton leftPanelContinueButton = new JButton("Continue");
        leftPanelContinueButton.addActionListener(actionEvent -> mainAgent.continuar());

        infoNumJugadores = new JLabel("Jugadores por partida: ");
        actualizarLabelJugadores();
        JButton botonActualizarJugadores = new JButton("Actualizar jugadores");
        botonActualizarJugadores.addActionListener(actionEvent -> mainAgent.updatePlayers());
        JButton botonEliminarJugadores = new JButton("Eliminar jugadores");
        botonEliminarJugadores.addActionListener(actionEvent -> mainAgent.removePlayer(JOptionPane.showInputDialog(new Frame("Remove player"), "Name of the player?")));

        MainAgent.GameParametersStruct parameters = mainAgent.new GameParametersStruct();
        leftPanelExtraInformation = new JLabel("Parameters: " + parameters.getParametros());

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;
        gc.weightx = 0.5;
        gc.weighty = 0.5;
        gc.gridy = 0;
        leftPanel.add(leftPanelRoundsLabel, gc);
        gc.gridy = 1;
        leftPanel.add(leftPanelGenerationLabel, gc);
        gc.gridy = 2;
        leftPanel.add(infoNumJugadores, gc);
        gc.gridy = 3;
        leftPanel.add(leftPanelNewButton, gc);
        gc.gridy = 4;
        leftPanel.add(leftPanelStopButton, gc);
        gc.gridy = 5;
        leftPanel.add(leftPanelContinueButton, gc);
        gc.gridy = 6;
        leftPanel.add(botonActualizarJugadores, gc);
        gc.gridy = 7;
        leftPanel.add(botonEliminarJugadores, gc);
        gc.gridy = 8;
        gc.weighty = 10;
        leftPanel.add(leftPanelExtraInformation, gc);

        return leftPanel;
    }

    private JPanel createCentralPanel() {
        JPanel centralPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;

        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;

        gc.gridy = 0;
        gc.weighty = 1;
        centralPanel.add(createCentralTopSubpanel(), gc);
        gc.gridy = 1;
        gc.weighty = 4;
        centralPanel.add(createCentralBottomSubpanel(), gc);

        return centralPanel;
    }

    private JPanel createCentralTopSubpanel() {
        JPanel centralTopSubpanel = new JPanel(new GridBagLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("Empty");
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(list);

        //JLabel info1 = new JLabel("Selected player info");

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.weighty = 0.5;
        gc.anchor = GridBagConstraints.CENTER;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 666;
        gc.fill = GridBagConstraints.BOTH;
        centralTopSubpanel.add(listScrollPane, gc);
        gc.gridx = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        //centralTopSubpanel.add(info1, gc);
        gc.gridy = 1;

        return centralTopSubpanel;
    }

    private JPanel createCentralBottomSubpanel() {

        JPanel centralBottomSubpanel = new JPanel(new GridBagLayout());
        //JTable payoffTable = new JTable (modelo);

        Object[] nullPointerWorkAround = {"*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*","*", "*", "*"};

        Object[][] data = {
                {"Jugador","", "", "", "", "", "", "", "", "", "", "PuntosRestantes", "Resultado", "Puntuacion"},
                {"player1","*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"player2","*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"player3","*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"player4","*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"player5","*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*", "*"}
                /*{"player1","*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"player1","*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"player1","*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"player1","*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"player1","*", "*", "*", "*", "*", "*", "*", "*", "*", "*"},
                {"player1","*", "*", "*", "*", "*", "*", "*", "*", "*", "*"}*/
                
        };
        

        JLabel payoffLabel = new JLabel("Player Results");
        payoffTable = new JTable(data, nullPointerWorkAround);
        payoffTable.setTableHeader(null);
        payoffTable.setEnabled(false);
        
        JScrollPane player1ScrollPane = new JScrollPane(payoffTable);

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weighty = 0.5;
        centralBottomSubpanel.add(payoffLabel, gc);
        gc.gridy = 1;
        gc.gridx = 0;
        gc.weighty = 2;
        centralBottomSubpanel.add(player1ScrollPane, gc);

        return centralBottomSubpanel;
    }

    

    private JPanel createRightPanel() {
        rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.weighty = 1d;
        c.weightx = 1d;

        rightPanelLoggingTextArea = new JTextArea("");
        rightPanelLoggingTextArea.setEditable(false);
        JScrollPane jScrollPane = new JScrollPane(rightPanelLoggingTextArea);
        rightPanel.add(jScrollPane, c);
        return rightPanel;
    }

    private JMenuBar createMainMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuEdit = new JMenu("Edit");
        JMenuItem resetPlayerEditMenu = new JMenuItem("Reset Players");
        resetPlayerEditMenu.setToolTipText("Reset all player");
        resetPlayerEditMenu.setActionCommand("reset_players");
        resetPlayerEditMenu.addActionListener(actionEvent -> mainAgent.updatePlayers());
        

        JMenuItem removePlayerEditMenu = new JMenuItem("Remove Player");
        removePlayerEditMenu.setToolTipText("Remove a player");
        removePlayerEditMenu.setActionCommand("remove_player");
        removePlayerEditMenu.addActionListener(actionEvent -> mainAgent.removePlayer(JOptionPane.showInputDialog(new Frame("Remove player"), "Name of the player?")));
        
        menuEdit.add(resetPlayerEditMenu);
        menuEdit.add(removePlayerEditMenu);
        menuBar.add(menuEdit);
        
        JMenu menuRun = new JMenu("Run");
        
        JMenuItem newRunMenu = new JMenuItem("New");
        newRunMenu.setToolTipText("Starts a new series of games");
        newRunMenu.addActionListener(this);
        
        JMenuItem stopRunMenu = new JMenuItem("Stop");
        stopRunMenu.setToolTipText("Stops the execution of the current round");
        stopRunMenu.addActionListener(actionEvent -> mainAgent.pausa());
        
        JMenuItem continueRunMenu = new JMenuItem("Continue");
        continueRunMenu.setToolTipText("Resume the execution");
        continueRunMenu.addActionListener(actionEvent -> mainAgent.continuar());
        
        JMenuItem roundNumberRunMenu = new JMenuItem("Number of games");
        roundNumberRunMenu.setToolTipText("Change the number of games");
        roundNumberRunMenu.addActionListener(this);
        
        JMenuItem parametersRunMenu = new JMenuItem("Parameters");
        parametersRunMenu.setToolTipText("Modify the parameters of the game");
        parametersRunMenu.addActionListener(this);



        menuRun.add(newRunMenu);
        menuRun.add(stopRunMenu);
        menuRun.add(continueRunMenu);
        menuRun.add(roundNumberRunMenu);
        menuRun.add(parametersRunMenu);
        menuBar.add(menuRun);

        JMenu menuWindow = new JMenu("Window");

        JCheckBoxMenuItem toggleVerboseWindowMenu = new JCheckBoxMenuItem("Verbose", true);
        toggleVerboseWindowMenu.addActionListener(actionEvent -> rightPanel.setVisible(toggleVerboseWindowMenu.getState()));

        menuWindow.add(toggleVerboseWindowMenu);
        menuBar.add(menuWindow);

        /***************Cosas nuevas****************/
        JMenu menuHelp = new JMenu("Help");
        JMenuItem aboutMenu = new JMenuItem("About");
        aboutMenu.addActionListener(actionEvent -> JOptionPane.showMessageDialog(null, "Realizado por Axel Valladares Pazó -- DNI:53973795V"));
        menuHelp.add(aboutMenu);
        menuBar.add(menuHelp);
        /*******************************************/

        return menuBar;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
            logLine("Button " + button.getText());

        } else if("Parameters".equals(e.getActionCommand())) {
            MainAgent.GameParametersStruct parameters = mainAgent.new GameParametersStruct();
            String[] params = JOptionPane.showInputDialog(new Frame("Configure parameters"), "Enter parameters N,E,R,I,P").split(","); //obtiene los valores del JPanel
            parameters.setParametros(Integer.parseInt(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2]), Float.parseFloat(params[3]), Integer.parseInt(params[4]), Integer.parseInt(params[5])); //actualiza las variables
            leftPanelExtraInformation.setText("Parameters: " + parameters.getParametros()); //actualiza la etiqueta de la GUI
            System.out.println("Parameters "+parameters.getParametros());
        
        } else if("Number of games".equals(e.getActionCommand())){
            MainAgent.GameParametersStruct parameters = mainAgent.new GameParametersStruct();
            String param = JOptionPane.showInputDialog(new Frame("Number of games"), "Enter parameter P"); //obtiene los valores del JPanel
            parameters.setP(Integer.parseInt(param)); //actualiza las variables
            leftPanelExtraInformation.setText("Parameters: " + parameters.getParametros()); //actualiza la etiqueta de la GUI

        }else if (e.getSource() instanceof JMenuItem) {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            logLine("Menu " + menuItem.getText());           
        }
    }

    public class LoggingOutputStream extends OutputStream {
        private JTextArea textArea;

        public LoggingOutputStream(JTextArea jTextArea) {
            textArea = jTextArea;
        }

        @Override
        public void write(int i) throws IOException {
            textArea.append(String.valueOf((char) i));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }


    /***************************** Actualiza el numero de jugadores en el panel de la izquierda *****************************/
    public void actualizarLabelJugadores() {

        Runnable setTextParameters = () -> {
            infoNumJugadores.setText("Jugadores: "+mainAgent.getN());
        };
        SwingUtilities.invokeLater(setTextParameters);

    }

    /*************************** Actualizar el numero de rondas ********************************/
    public void actualizarNumRondas(int ronda, int partida) {
        Runnable cambiarLabel = () -> {
            leftPanelRoundsLabel.setText("Round " + (ronda+1) + " / " + (partida+1));
        };
        SwingUtilities.invokeLater(cambiarLabel);
    }

    /*************************** Actualizar el numero de generaciones ********************************/
    public void actualizarNumGeneracion(int generacion) {
        Runnable cambiarGeneracion = () -> {
            leftPanelGenerationLabel.setText("Generation: " + (generacion+1));
        };
        SwingUtilities.invokeLater(cambiarGeneracion);
    }


    /*************************** Actualizar en la tabla loas apuestas de cada uno ********************************/
    public void setActualizarTabla (int action, int playerID, int round){
        payoffTable.setValueAt(action, playerID+1, round);
    }
    

    /************************************Actualizar en la tabla los puntos de un jugador ********************************/
    public void setActualizarResultados (int puntos, int id){
        payoffTable.setValueAt(puntos, id+1, 11);
    }


    /*********************************** Actualizar en la tabla la columna de resultados al jugador que corresponda *************/
    public void setActualizarGanador(int id){  
        payoffTable.setValueAt((int)payoffTable.getValueAt(id+1, 12)+1, id+1, 12);
    }

    /********************************** Actualiza en la tabla la columna de puntos totales del jugador que corresponda *********************/
    public void setActualizarAportacion(int aportacion, int id){  
        payoffTable.setValueAt(aportacion, id+1, 13);
    }

    /*********************************** Poner a 0 la columna de resultados de cada jugador **************************/
    public void setResetColumnaPartidas(){
        for(int i=0; i<5; i++){
            payoffTable.setValueAt(0, i+1, 12);
        }
    }

    /********************************* Saca por pantalla el podium tras finalizar el torneo ***********************/
    public void ganador(String primero, String segundo, String tercero){
        String respuesta = "Ganador del torneo el jugador " + primero + "\n" + "Segunda posicion: " + segundo + "\n" + "Tercera posicion: " + tercero;
        JOptionPane.showMessageDialog(null, respuesta);
    }


    /***************************** Pone a 0 toda la tabla *************************/
    public void resetTabla(){
        for(int x=1; x<6; x++){
            for(int y=1; y<12; y++){
                payoffTable.setValueAt(0, x, y);
            }
        }
    }

}
