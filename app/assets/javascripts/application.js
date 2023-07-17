// when ready...
document.addEventListener('DOMContentLoaded', function () {

  //add alert before for destroying planet
  if(document.getElementById("link_planet_destroy") !== null) {
    document.getElementById("link_planet_destroy").addEventListener("click", function(){
        if(confirm('Destroy current planet and lose the associated data?')){
        document.getElementById('destroyPlanet').submit()
        };
    });
  }


  //do other js stuff...

}, false);