sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'thread/test/integration/FirstJourney',
		'thread/test/integration/pages/ThreadList',
		'thread/test/integration/pages/ThreadObjectPage'
    ],
    function(JourneyRunner, opaJourney, ThreadList, ThreadObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('thread') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheThreadList: ThreadList,
					onTheThreadObjectPage: ThreadObjectPage
                }
            },
            opaJourney.run
        );
    }
);