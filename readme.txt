I HAVE READ AND COMPLY WITH ALL THE RECOMMENDATIONS PROVIDED AT THE LAB FORUM !!!

Axel Valladares Pazó: ID = 26

- To compile (en la carpeta raiz y en la de agents): javac -cp .:jade.jar .java 
- To execute Random:  java -cp .:jade.jar jade.Boot -notmp -gui -agents "MainAgent:MainAgent;player1:agents.RandomAgent;player2:agents.RandomAgent;player3:agents.RandomAgent;player4:agents.RandomAgent;player5:agents.RandomAgent;player6:agents.RandomAgent;player7:agents.RandomAgent;"
- To execute RL:  java -cp .:jade.jar jade.Boot -notmp -gui -agents "MainAgent:MainAgent;player1:agents.RL_Agent;player2:agents.RL_Agent;player3:agents.RL_Agent;player4:agents.RL_Agent;player5:agents.RL_Agent;player6:agents.RL_Agent;player7:agents.RL_Agent;"
- To execute NN:  java -cp .:jade.jar jade.Boot -notmp -gui -agents "MainAgent:MainAgent;player1:agents.NN_Agent;player2:agents.NN_Agent;player3:agents.NN_Agent;player4:agents.NN_Agent;player5:agents.NN_Agent;player6:agents.NN_Agent;player7:agents.NN_Agent;"

En RL se ha implementado Learning Autómata pero con matriz de intervalos, es decir, por cada jugada posible por ronda (columnas) hay un total de 6 filas. Entre
la fila 0 y fila 1 comprende el intervalo de probabilidad de jugar "0", entre la fila 1 y fila 2 la probabilidad de jugar "1" y así hasta 4.
Cuando la matriz aprende, estes intervalos se van aumentando o disminuyendo dependiendo de si la jugada fue exitosa o no en base a si se perdió por catastrofe o si el agente 
fue ganador de la partida.

En NN se ha implementado SOM con una matriz de pesos y como vector input las jugadas realizadas en la partida anterior. Al terminar la partida se refuerzan los pesos apropiados
y sus vecinos adyacentes (en vertical y horizontal). 
Se comprueba en la matriz de LA y se hace la jugada pertinente dependiendo de la ronda en la que se encuentren.


Para el torneo he decidio usar al RL (learning automata descrito arriba) ya que ha sido el que mejores resultados me ha dado.