# TORCS-QLearning

Proyecto de Aprendizaje por Refuerzo donde entrenamos un coche en TORCS utilizando Q-Learning para controlar:

- Marchas (Gear)
- Volante (Steering)
- Acelerador (Accel)
- Freno (Brake)

Este proyecto implementa un agente basado en Q-Learning que interactúa con el simulador TORCS.
El agente aprende una política óptima mediante exploración y explotación, actualizando su función Q en base a recompensas obtenidas durante la conducción.

El sistema incluye:

- Definición del espacio de estados
- Espacio de acciones discretizado
- Función de recompensa personalizada
- Estrategia ε-greedy
- Entrenamiento y evaluación del agente
