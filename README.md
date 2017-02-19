# Big Cash
Simple realisation of Big Cash slot machine game using Scala and Akka


To start server please run BigCashServerRunner class.
You can configure server with server.conf


To start bot game client please run BigCashClientRunner class.
You can configure client with client.conf
To set player id for client bot you can pass it as program argument, otherwise current unix timestamp will be used as user id.
You can setup bot behaviour as function ServerMessage => Boolean


The process of client-server interaction:  
1) Client connects to server and writes initial message   
2) Server responds with the list of banknotes available in BigCash game   
3) Client responds with accept=true if wants to start game, accept=false otherwise   
4) After accepting game Client starts receiving offers from Server, for every offer Client can respond accept=true or accept=false  
5) If Client answers true or if declines for the 3 times (== automatically choosing 4 offer) Server updates client statistics and responds with moneyWon


Current realisation uses MongoDB as game statistics storage

Author: Artem Vedernikov

