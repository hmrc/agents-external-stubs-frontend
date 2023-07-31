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

  //trigger auto populate enrolment based on affinity type
  if(document.getElementById("initialUserDataForm") !== null) {
   var affinityGroupNone = document.getElementById("affinityGroup");
   var affinityGroupIndividual = document.getElementById("affinityGroup-2");
   var affinityGroupOrg = document.getElementById("affinityGroup-3");
   var affinityGroupAgent = document.getElementById("affinityGroup-4");

   affinityGroupNone.addEventListener("input", function() {filterServices("none")}    );
   affinityGroupIndividual.addEventListener("input", function() {filterServices("Individual")}    );
   affinityGroupOrg.addEventListener("input",  function() {filterServices("Organisation") });
   affinityGroupAgent.addEventListener("input",  function() {filterServices("Agent")});

     //when called auto populate enrolment based on affinity type
      function filterServices(keyword){
      console.log("triggered " + keyword)
           var select = document.getElementById("principalEnrolmentService");
           var selectedIndex = 0
           for (var i = 0; i < select.length; i++) {
               var txt = select.options[i].dataset.affinity;
               var include = txt==='none' | txt.toLowerCase().indexOf(keyword.toLowerCase())>=0;
               select.options[i].style.display = include ? 'list-item':'none';
               select.options[i].checked = (keyword === "Agent" && select.options[i].value === "HMRC-AS-AGENT") ||
                       (keyword === "Individual" && select.options[i].value === "HMRC-MTD-IT") ||
                       (keyword === "Organisation" && select.options[i].value === "HMRC-MTD-VAT") ||
                       (keyword ==="none" && select.options[i].value === "none") ||
                       false;
               if(select.options[i].checked) selectedIndex = i;
           }
           select.selectedIndex = selectedIndex;
       }

  }

  //do other js stuff...

}, false);
