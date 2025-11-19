// when ready...
document.addEventListener('DOMContentLoaded', function () {

    //add alert before for destroying planet
    if (document.getElementById("link_planet_destroy") !== null) {
        document.getElementById("link_planet_destroy").addEventListener("click", function () {
            if (confirm('Destroy current planet and lose the associated data?')) {
                document.getElementById('destroyPlanet').submit()
            }
            ;
        });
    }

    //trigger auto populate enrolment based on affinity type
    if (document.getElementById("initialUserDataForm") !== null) {
        var affinityGroupNone = document.getElementById("affinityGroup");
        var affinityGroupIndividual = document.getElementById("affinityGroup-2");
        var affinityGroupOrg = document.getElementById("affinityGroup-3");
        var affinityGroupAgent = document.getElementById("affinityGroup-4");

        affinityGroupNone.addEventListener("input", function () {
            filterServices("none")
        });
        affinityGroupIndividual.addEventListener("input", function () {
            filterServices("Individual")
        });
        affinityGroupOrg.addEventListener("input", function () {
            filterServices("Organisation")
        });
        affinityGroupAgent.addEventListener("input", function () {
            filterServices("Agent")
        });

        //when called auto populate enrolment based on affinity type
        function filterServices(keyword) {
            console.log("triggered " + keyword)
            var select = document.getElementById("principalEnrolmentService");
            var selectedIndex = 0
            for (var i = 0; i < select.length; i++) {
                var txt = select.options[i].dataset.affinity;
                var include = txt === 'none' | txt.toLowerCase().indexOf(keyword.toLowerCase()) >= 0;
                select.options[i].style.display = include ? 'list-item' : 'none';
                select.options[i].checked = (keyword === "Agent" && select.options[i].value === "HMRC-AS-AGENT") ||
                    (keyword === "Individual" && select.options[i].value === "HMRC-MTD-IT") ||
                    (keyword === "Organisation" && select.options[i].value === "HMRC-MTD-VAT") ||
                    (keyword === "none" && select.options[i].value === "none") ||
                    false;
                if (select.options[i].checked) selectedIndex = i;
            }
            select.selectedIndex = selectedIndex;
        }

    }
    if (document.getElementById("js-principal-enrolments-table") !== null) {
        document.querySelectorAll('#js-principal-enrolments-table tbody tr:not(:first-child)').forEach((row, index) => {
            const removeButton = document.querySelector(`#js-remove-principal-enrolment-${index}`);
            if (removeButton) {
                removeButton.addEventListener('click', () => removePrincipalEnrolment(index));
            }
        });
    }

    if (document.getElementById("js-delegated-enrolments-table") !== null) {
        document.querySelectorAll('#js-delegated-enrolments-table tbody tr:not(:first-child)').forEach((row, index) => {
            const removeButton = document.querySelector(`#js-remove-delegated-enrolment-${index}`);
            console.log("Remove button: " + removeButton +". Row: " + row + ". Index: " + index);
            if (removeButton) {
                console.log("Remove button: " + removeButton +". Row: " + row + ". Index: " + index);
                removeButton.addEventListener('click', () => removeDelegatedEnrolment(index));
            }
        });
    }

    // === A11y: give a unique name to the utility "Sign out" nav landmark ===
    (function () {
        function labelSignOutNav() {
            var nodes = document.querySelectorAll('nav.hmrc-sign-out-nav');
            nodes.forEach(function (el) {
                if (!el.getAttribute('aria-label') && !el.getAttribute('aria-labelledby')) {
                    el.setAttribute('aria-label', 'Sign out');
                }
            });
        }

        function onReady(fn) {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', fn);
            } else {
                fn();
            }
        }

        onReady(labelSignOutNav);

        // Optional: if your pages swap content dynamically, keep labels applied.
        if ('MutationObserver' in window) {
            var mo = new MutationObserver(labelSignOutNav);
            mo.observe(document.documentElement, {childList: true, subtree: true});
        }
    })();

    function removePrincipalEnrolment(index) {
        document.getElementById(`assignedPrincipalEnrolments-${index}-row`).remove();
    }

    function removeDelegatedEnrolment(index) {
        const rows = document.querySelectorAll(`#assignedDelegatedEnrolments-${index}-row`);
        rows.forEach(row => row.remove());
    }

    //do other js stuff...

}, false);
