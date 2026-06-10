1. GET Fixtures (https://v3.football.api-sports.io/fixtures)

For all requests to fixtures you can add the query parameter timezone to your request in order to retrieve the list of matches in the time zone of your choice like “Europe/London“

To know the list of available time zones you have to use the endpoint timezone.


query Parameters
id	
integer
Value: "id"
The id of the fixture

ids	
stringMaximum of 20 fixtures ids
Value: "id-id-id"
One or more fixture ids

live	
string
Enum: "all" "id-id"
All or several leagues ids

date	
stringYYYY-MM-DD
A valid date

league	
integer
The id of the league

season	
integer = 4 characters YYYY
The season of the league

team	
integer
The id of the team

last	
integer <= 2 characters
For the X last fixtures

next	
integer <= 2 characters
For the X next fixtures

from	
stringYYYY-MM-DD
A valid date

to	
stringYYYY-MM-DD
A valid date

round	
string
The round of the fixture

status	
string
Enum: "NS" "NS-PST-FT"
One or more fixture status short

venue	
integer
The venue id of the fixture

timezone	
string
A valid timezone from the endpoint Timezone

header Parameters
x-apisports-key
required
string
Your Api-Key



Response :

{
  "get": "fixtures",
  "parameters": {
    "live": "all"
  },
  "errors": [],
  "results": 4,
  "paging": {
    "current": 1,
    "total": 1
  },
  "response": [
    {
      "fixture": {
        "id": 239625,
        "referee": null,
        "timezone": "UTC",
        "date": "2020-02-06T14:00:00+00:00",
        "timestamp": 1580997600,
        "periods": {
          "first": 1580997600,
          "second": null
        },
        "venue": {
          "id": 1887,
          "name": "Stade Municipal",
          "city": "Oued Zem"
        },
        "status": {
          "long": "Halftime",
          "short": "HT",
          "elapsed": 45,
          "extra": null
        }
      },
      "league": {
        "id": 200,
        "name": "Botola Pro",
        "country": "Morocco",
        "logo": "https://media.api-sports.io/football/leagues/115.png",
        "flag": "https://media.api-sports.io/flags/ma.svg",
        "season": 2019,
        "round": "Regular Season - 14"
      },
      "teams": {
        "home": {
          "id": 967,
          "name": "Rapide Oued ZEM",
          "logo": "https://media.api-sports.io/football/teams/967.png",
          "winner": false
        },
        "away": {
          "id": 968,
          "name": "Wydad AC",
          "logo": "https://media.api-sports.io/football/teams/968.png",
          "winner": true
        }
      },
      "goals": {
        "home": 0,
        "away": 1
      },
      "score": {
        "halftime": {
          "home": 0,
          "away": 1
        },
        "fulltime": {
          "home": null,
          "away": null
        },
        "extratime": {
          "home": null,
          "away": null
        },
        "penalty": {
          "home": null,
          "away": null
        }
      }
    }
  ]
}

-------------------------------


2. GET Fixtures Stats https://v3.football.api-sports.io/fixtures/statistics

query Parameters
fixture
required
integer
The id of the fixture

team	
integer
The id of the team

type	
string
The type of statistics

half	
boolean
Default: false
Enum: "true" "false"
Add the halftime statistics in the response Data start from 2024 season for half parameter

Response:
{
  "get": "fixtures/statistics",
  "parameters": {
    "team": "463",
    "fixture": "215662"
  },
  "errors": [],
  "results": 1,
  "paging": {
    "current": 1,
    "total": 1
  },
  "response": [
    {
      "team": {
        "id": 463,
        "name": "Aldosivi",
        "logo": "https://media.api-sports.io/football/teams/463.png"
      },
      "statistics": [
        {
          "type": "Shots on Goal",
          "value": 3
        },
        {
          "type": "Shots off Goal",
          "value": 2
        },
        {
          "type": "Total Shots",
          "value": 9
        },
        {
          "type": "Blocked Shots",
          "value": 4
        },
        {
          "type": "Shots insidebox",
          "value": 4
        },
        {
          "type": "Shots outsidebox",
          "value": 5
        },
        {
          "type": "Fouls",
          "value": 22
        },
        {
          "type": "Corner Kicks",
          "value": 3
        },
        {
          "type": "Offsides",
          "value": 1
        },
        {
          "type": "Ball Possession",
          "value": "32%"
        },
        {
          "type": "Yellow Cards",
          "value": 5
        },
        {
          "type": "Red Cards",
          "value": 1
        },
        {
          "type": "Goalkeeper Saves",
          "value": null
        },
        {
          "type": "Total passes",
          "value": 242
        },
        {
          "type": "Passes accurate",
          "value": 121
        },
        {
          "type": "Passes %",
          "value": null
        }
      ]
    }
  ]
}



3. GET EVENTS https://v3.football.api-sports.io/fixtures/events
Get the events from a fixture.

Available events

TYPE				
Goal	Normal Goal	Own Goal	Penalty	Missed Penalty
Card	Yellow Card	Red card		
Subst	Substitution [1, 2, 3...]			
Var	Goal cancelled	Penalty confirmed		
VAR events are available from the 2020-2021 season.


query Parameters
fixture
required
integer
The id of the fixture

team	
integer
The id of the team

player	
integer
The id of the player

type	
string
The type

header Parameters
x-apisports-key
required
string
Your Api-Key


Response 

{
  "get": "fixtures/events",
  "parameters": {
    "fixture": "215662"
  },
  "errors": [],
  "results": 18,
  "paging": {
    "current": 1,
    "total": 1
  },
  "response": [
    {
      "time": {
        "elapsed": 25,
        "extra": null
      },
      "team": {
        "id": 463,
        "name": "Aldosivi",
        "logo": "https://media.api-sports.io/football/teams/463.png"
      },
      "player": {
        "id": 6126,
        "name": "F. Andrada"
      },
      "assist": {
        "id": null,
        "name": null
      },
      "type": "Goal",
      "detail": "Normal Goal",
      "comments": null
    },
    {
      "time": {
        "elapsed": 33,
        "extra": null
      },
      "team": {
        "id": 442,
        "name": "Defensa Y Justicia",
        "logo": "https://media.api-sports.io/football/teams/442.png"
      },
      "player": {
        "id": 5936,
        "name": "Julio González"
      },
      "assist": {
        "id": null,
        "name": null
      },
      "type": "Card",
      "detail": "Yellow Card",
      "comments": null
    },
    {
      "time": {
        "elapsed": 33,
        "extra": null
      },
      "team": {
        "id": 463,
        "name": "Aldosivi",
        "logo": "https://media.api-sports.io/football/teams/463.png"
      },
      "player": {
        "id": 6126,
        "name": "Federico Andrada"
      },
      "assist": {
        "id": null,
        "name": null
      },
      "type": "Card",
      "detail": "Yellow Card",
      "comments": null
    },
    {
      "time": {
        "elapsed": 73,
        "extra": null
      },
      "team": {
        "id": 442,
        "name": "Defensa Y Justicia",
        "logo": "https://media.api-sports.io/football/teams/442.png"
      },
      "player": {
        "id": 5942,
        "name": "A. Castro"
      },
      "assist": {
        "id": 6059,
        "name": "G. Mainero"
      },
      "type": "subst",
      "detail": "Substitution 2",
      "comments": null
    },
    {
      "time": {
        "elapsed": 74,
        "extra": null
      },
      "team": {
        "id": 463,
        "name": "Aldosivi",
        "logo": "https://media.api-sports.io/football/teams/463.png"
      },
      "player": {
        "id": 6561,
        "name": "N. Solís"
      },
      "assist": {
        "id": 35845,
        "name": "H. Burbano"
      },
      "type": "subst",
      "detail": "Substitution 1",
      "comments": null
    },
    {
      "time": {
        "elapsed": 75,
        "extra": null
      },
      "team": {
        "id": 463,
        "name": "Aldosivi",
        "logo": "https://media.api-sports.io/football/teams/463.png"
      },
      "player": {
        "id": 6093,
        "name": "G. Verón"
      },
      "assist": {
        "id": 6396,
        "name": "N. Bazzana"
      },
      "type": "subst",
      "detail": "Substitution 2",
      "comments": null
    },
    {
      "time": {
        "elapsed": 90,
        "extra": null
      },
      "team": {
        "id": 463,
        "name": "Aldosivi",
        "logo": "https://media.api-sports.io/football/teams/463.png"
      },
      "player": {
        "id": 35845,
        "name": "Hernán Burbano"
      },
      "assist": {
        "id": null,
        "name": null
      },
      "type": "Card",
      "detail": "Red Card",
      "comments": null
    },
    {
      "time": {
        "elapsed": 90,
        "extra": null
      },
      "team": {
        "id": 463,
        "name": "Aldosivi",
        "logo": "https://media.api-sports.io/football/teams/463.png"
      },
      "player": {
        "id": 35845,
        "name": "Hernán Burbano"
      },
      "assist": {
        "id": null,
        "name": null
      },
      "type": "Card",
      "detail": "Yellow Card",
      "comments": null
    }
  ]
}



------------------------------------------



4. Lineups  https://v3.football.api-sports.io/fixtures/lineups 

query Parameters
fixture
required
integer
The id of the fixture

team	
integer
The id of the team

player	
integer
The id of the player

type	
string
The type

header Parameters
x-apisports-key
required
string
Your Api-Key


Response:

{
  "get": "fixtures/lineups",
  "parameters": {
    "fixture": "592872"
  },
  "errors": [],
  "results": 2,
  "paging": {
    "current": 1,
    "total": 1
  },
  "response": [
    {
      "team": {
        "id": 50,
        "name": "Manchester City",
        "logo": "https://media.api-sports.io/football/teams/50.png",
        "colors": {
          "player": {
            "primary": "5badff",
            "number": "ffffff",
            "border": "99ff99"
          },
          "goalkeeper": {
            "primary": "99ff99",
            "number": "000000",
            "border": "99ff99"
          }
        }
      },
      "formation": "4-3-3",
      "startXI": [
        {
          "player": {
            "id": 617,
            "name": "Ederson",
            "number": 31,
            "pos": "G",
            "grid": "1:1"
          }
        },
        {
          "player": {
            "id": 627,
            "name": "Kyle Walker",
            "number": 2,
            "pos": "D",
            "grid": "2:4"
          }
        },
        {
          "player": {
            "id": 626,
            "name": "John Stones",
            "number": 5,
            "pos": "D",
            "grid": "2:3"
          }
        },
        {
          "player": {
            "id": 567,
            "name": "Rúben Dias",
            "number": 3,
            "pos": "D",
            "grid": "2:2"
          }
        },
        {
          "player": {
            "id": 641,
            "name": "Oleksandr Zinchenko",
            "number": 11,
            "pos": "D",
            "grid": "2:1"
          }
        },
        {
          "player": {
            "id": 629,
            "name": "Kevin De Bruyne",
            "number": 17,
            "pos": "M",
            "grid": "3:3"
          }
        },
        {
          "player": {
            "id": 640,
            "name": "Fernandinho",
            "number": 25,
            "pos": "M",
            "grid": "3:2"
          }
        },
        {
          "player": {
            "id": 631,
            "name": "Phil Foden",
            "number": 47,
            "pos": "M",
            "grid": "3:1"
          }
        },
        {
          "player": {
            "id": 635,
            "name": "Riyad Mahrez",
            "number": 26,
            "pos": "F",
            "grid": "4:3"
          }
        },
        {
          "player": {
            "id": 643,
            "name": "Gabriel Jesus",
            "number": 9,
            "pos": "F",
            "grid": "4:2"
          }
        },
        {
          "player": {
            "id": 645,
            "name": "Raheem Sterling",
            "number": 7,
            "pos": "F",
            "grid": "4:1"
          }
        }
      ],
      "substitutes": [
        {
          "player": {
            "id": 50828,
            "name": "Zack Steffen",
            "number": 13,
            "pos": "G",
            "grid": null
          }
        },
        {
          "player": {
            "id": 623,
            "name": "Benjamin Mendy",
            "number": 22,
            "pos": "D",
            "grid": null
          }
        },
        {
          "player": {
            "id": 18861,
            "name": "Nathan Aké",
            "number": 6,
            "pos": "D",
            "grid": null
          }
        },
        {
          "player": {
            "id": 622,
            "name": "Aymeric Laporte",
            "number": 14,
            "pos": "D",
            "grid": null
          }
        },
        {
          "player": {
            "id": 633,
            "name": "İlkay Gündoğan",
            "number": 8,
            "pos": "M",
            "grid": null
          }
        },
        {
          "player": {
            "id": 44,
            "name": "Rodri",
            "number": 16,
            "pos": "M",
            "grid": null
          }
        },
        {
          "player": {
            "id": 931,
            "name": "Ferrán Torres",
            "number": 21,
            "pos": "F",
            "grid": null
          }
        },
        {
          "player": {
            "id": 636,
            "name": "Bernardo Silva",
            "number": 20,
            "pos": "M",
            "grid": null
          }
        },
        {
          "player": {
            "id": 642,
            "name": "Sergio Agüero",
            "number": 10,
            "pos": "F",
            "grid": null
          }
        }
      ],
      "coach": {
        "id": 4,
        "name": "Guardiola",
        "photo": "https://media.api-sports.io/football/coachs/4.png"
      }
    },
    {
      "team": {
        "id": 45,
        "name": "Everton",
        "logo": "https://media.api-sports.io/football/teams/45.png",
        "colors": {
          "player": {
            "primary": "070707",
            "number": "ffffff",
            "border": "66ff00"
          },
          "goalkeeper": {
            "primary": "66ff00",
            "number": "000000",
            "border": "66ff00"
          }
        }
      },
      "formation": "4-3-1-2",
      "startXI": [
        {
          "player": {
            "id": 2932,
            "name": "Jordan Pickford",
            "number": 1,
            "pos": "G",
            "grid": "1:1"
          }
        },
        {
          "player": {
            "id": 19150,
            "name": "Mason Holgate",
            "number": 4,
            "pos": "D",
            "grid": "2:4"
          }
        },
        {
          "player": {
            "id": 2934,
            "name": "Michael Keane",
            "number": 5,
            "pos": "D",
            "grid": "2:3"
          }
        },
        {
          "player": {
            "id": 19073,
            "name": "Ben Godfrey",
            "number": 22,
            "pos": "D",
            "grid": "2:2"
          }
        },
        {
          "player": {
            "id": 2724,
            "name": "Lucas Digne",
            "number": 12,
            "pos": "D",
            "grid": "2:1"
          }
        },
        {
          "player": {
            "id": 18805,
            "name": "Abdoulaye Doucouré",
            "number": 16,
            "pos": "M",
            "grid": "3:3"
          }
        },
        {
          "player": {
            "id": 326,
            "name": "Allan",
            "number": 6,
            "pos": "M",
            "grid": "3:2"
          }
        },
        {
          "player": {
            "id": 18762,
            "name": "Tom Davies",
            "number": 26,
            "pos": "M",
            "grid": "3:1"
          }
        },
        {
          "player": {
            "id": 2795,
            "name": "Gylfi Sigurðsson",
            "number": 10,
            "pos": "M",
            "grid": "4:1"
          }
        },
        {
          "player": {
            "id": 18766,
            "name": "Dominic Calvert-Lewin",
            "number": 9,
            "pos": "F",
            "grid": "5:2"
          }
        },
        {
          "player": {
            "id": 2413,
            "name": "Richarlison",
            "number": 7,
            "pos": "F",
            "grid": "5:1"
          }
        }
      ],
      "substitutes": [
        {
          "player": {
            "id": 18755,
            "name": "João Virgínia",
            "number": 31,
            "pos": "G",
            "grid": null
          }
        },
        {
          "player": {
            "id": 766,
            "name": "Robin Olsen",
            "number": 33,
            "pos": "G",
            "grid": null
          }
        },
        {
          "player": {
            "id": 156490,
            "name": "Niels Nkounkou",
            "number": 18,
            "pos": "D",
            "grid": null
          }
        },
        {
          "player": {
            "id": 18758,
            "name": "Séamus Coleman",
            "number": 23,
            "pos": "D",
            "grid": null
          }
        },
        {
          "player": {
            "id": 138849,
            "name": "Kyle John",
            "number": 48,
            "pos": "D",
            "grid": null
          }
        },
        {
          "player": {
            "id": 18765,
            "name": "André Gomes",
            "number": 21,
            "pos": "M",
            "grid": null
          }
        },
        {
          "player": {
            "id": 1455,
            "name": "Alex Iwobi",
            "number": 17,
            "pos": "F",
            "grid": null
          }
        },
        {
          "player": {
            "id": 18761,
            "name": "Bernard",
            "number": 20,
            "pos": "F",
            "grid": null
          }
        }
      ],
      "coach": {
        "id": 2407,
        "name": "C. Ancelotti",
        "photo": "https://media.api-sports.io/football/coachs/2407.png"
      }
    }
  ]
}