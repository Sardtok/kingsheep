<!DOCTYPE HTML>

<html>
 <head>
  <title>King Sheep Ladder</title>
  <style type="text/css">
    .center { margin-left: auto; margin-right: auto; width: 1280px;  }
    .huge { font-size: 125%; }
    .highlight { background: #afa; }
    table { margin-left: auto; margin-right: auto; border-collapse: collapse; }
    thead { font-weight: bold; }
    thead td { background: #eef; text-align: center; }
    td { padding: 0.5em 1em; text-align: right; border: 1px solid black; }
    h1 { background: url(Background.png); width: 1280px; height: 480px; text-align: center; vertical-align: bottom; }
    h2 { width: 1280px; text-align: center; }
  </style>
  <script src="sorttable.js"></script>
 </head>
 <body>
  <h1 class="center">King Sheep Ladder</h1>
  <?php
    $dir = substr($_SERVER['SCRIPT_FILENAME'], 0,
                  strrpos($_SERVER['SCRIPT_FILENAME'], 'index.php'));
    $dirs = preg_split("#/#", $dir);
    $scriptdir = $dirs[count($dirs) - 2];

    $request = preg_split("#/#", $_SERVER['REQUEST_URI']);
    $count = count($request);
    $firstarg = 0;
    for ($i = 0; $i < $count; $i++) {
     if (strcasecmp($request[$i], $scriptdir) == 0) {
      $firstarg = ++$i;
      break;
     }
    }

    $team = $request[$firstarg];
    if($team != null) {
      echo "<h2 class=\"center\">Stats for $team</h2>";
      echo "<table>";
    } else {
      echo "<table class=\"sortable\">";
    }
   ?>
   <thead>
    <tr>
     <td>Team</td>
     <td>Wins</td>
     <td>Losses</td>
     <td>Draws</td>
     <td>Grass eaten</td>
     <td>Rhubarb eaten</td>
     <td>Grass crushed</td>
     <td>Rhubarb crushed</td>
     <td>Sheep eaten</td>
     <td>Average think time</td>
    </tr>
   </thead>
   <tbody>

    <?php
      $teams = array();
      loadTotals();

      if ($team != null)
        loadTeam($team);
      else
        printTotals();

      function getTime($seconds, $nanos, $moves) {
       $avg = 0;
       for ($i = 0; $i < $seconds; $i++) {
        $avg += 1000000000 / $moves;
       }

       $avg += $nanos / $moves;
       return $avg;
      }

      function loadTotals() {
        global $teams;
        $statsfile = @fopen("stats.csv", "r")
          or die('Statistics not available');
        while(($line = fgets($statsfile)) != false) {
          $stats = split(";", $line);
          
          if (!isset($teams[$stats[0]])) {
            $teams[$stats[0]] = array();
            $teams[$stats[0]][0] = $stats[0];
          }
          
          for ($i = 1; $i < sizeof($stats); $i++) {
            $teams[$stats[0]][$i] += $stats[$i];
          }
        }
        
        ksort($teams);
      }

      function printTotals() {
        global $teams;
        $best = array();
        foreach($teams as $team) {
          if ($team[1] > $teams[$best[1]][1]
              || !isset($best[1])) {
            $best[0] = $team[0];
            $best[1] = $team[0];
          }

          if ($team[2] < $teams[$best[2]][2]
              || !isset($best[2]))
            $best[2] = $team[0];

          if ($team[3] < $teams[$best[3]][3]
              || !isset($best[3]))
            $best[3] = $team[0];

          if ($team[4] > $teams[$best[4]][4]
              || !isset($best[4]))
            $best[4] = $team[0];

          if ($team[5] > $teams[$best[5]][5]
              || !isset($best[5]))
            $best[5] = $team[0];

          if ($team[7] < $teams[$best[6]][7]
              || !isset($best[6]))
            $best[6] = $team[0];

          if ($team[8] < $teams[$best[7]][8]
              || !isset($best[7]))
            $best[7] = $team[0];

          if ($team[6] > $teams[$best[8]][6]
              || !isset($best[8])) {
            $best[8] = $team[0];
          }

          if (!isset($best[9])) {
            $best[9] = $team[0];
          } else {
            $teamtime = round(getTime($team[11], $team[12], $team[13]));
            $besttime = round(getTime($teams[$best[9]][11],
                                      $teams[$best[9]][12],
                                      $teams[$best[9]][13]));
            if ($teamtime < $besttime) {
              $best[9] = $team[0];
            }
          }
        }

        foreach($teams as $team) {
          $highlights = array();
          for ($i = 0; $i < count($best); $i++) {
            if ($best[$i] == $team[0]) {
              $highlights[$i] = true;
            }
          }
          printStatsArray($team, $highlights, true);
        }
      }

      function loadTeam($team) {
        global $teams;
        $totals = $teams[$team];
        $match = 1;
        $statsfile = @fopen("stats.csv", "r")
          or die('Statistics not available');

        printStatsArray($totals);

        while (($line1 = fgets($statsfile)) != false
              && ($line2 = fgets($statsfile)) != false) {
          $stats1 = split(";", $line1);
          $stats2 = split(";", $line2);

          if ($stats2[0] == $team) {
            $tmp = $stats1;
            $stats1 = $stats2;
            $stats2 = $tmp;
          }

          if ($stats1[0] == $team) {
     ?>
    </tbody>
    <thead class="huge">
     <tr>
     <td colspan="10">
     <?php
              echo "Match #$match - $stats1[0] vs. $stats2[0]";
              $match++;
      ?>
     </td>
     </tr>
    </thead>
    <tbody>
    <?php
         printStatsArray($stats1);
              printStatsArray($stats2);
          }
        }
      }

function printStatsArray($stats, $highlights, $links = false) {
        echo '<tr>';
        echo '<td style="text-align: left;">';
        if($links) {
          echo "<a href=\"/sigmunha/kingsheep/$stats[0]\">$stats[0]</a>";
        } else {
          echo $stats[0];
        }
        echo '</td>';
        echo "<td " . ($highlights[1] ? 'class="highlight"' : '') . ">$stats[1]</td>";
        echo "<td " . ($highlights[2] ? 'class="highlight"' : '') . ">$stats[2]</td>";
        echo "<td " . ($highlights[3] ? 'class="highlight"' : '') . ">$stats[3]</td>";
        echo "<td " . ($highlights[4] ? 'class="highlight" ' : '')
          . "sorttable_customkey=\"$stats[4]\">$stats[4] / $stats[9]</td>";
        echo "<td " . ($highlights[5] ? 'class="highlight" ' : '')
          . "sorttable_customkey=\"$stats[5]\">$stats[5] / $stats[10]</td>";
        echo "<td " . ($highlights[6] ? 'class="highlight"' : '')
          . "sorttable_customkey=\"$stats[7]\">$stats[7] / $stats[9]</td>";
        echo "<td " . ($highlights[7] ? 'class="highlight"' : '')
          . "sorttable_customkey=\"$stats[8]\">$stats[8] / $stats[10]</td>";
        echo "<td " . ($highlights[8] ? 'class="highlight"' : '')
          . ">$stats[6]</td>";
        $avg = getTime($stats[11], $stats[12], $stats[13]);
        echo "<td " . ($highlights[9] ? 'class="highlight"' : '')
          . "sorttable_customkey=\"$avg\">"
          . ($avg > 1000000 ? round($avg / 1000000) . " ms</td>" :
            ($avg > 1000 ? round($avg / 1000) . " &mu;s</td>" :
             round($avg) . " ns</td>"));
        echo '</tr>';
      }
     ?>

   </tbody>
  </table>
 </body>
</html>
