package agent;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.ImmutableMap;

import adx.agent.AgentLogic;
import adx.exceptions.AdXException;
import adx.server.OfflineGameServer;
import adx.structures.SimpleBidEntry;
import adx.util.AgentStartupUtil;
import adx.structures.Campaign;
import adx.structures.MarketSegment;
import adx.variants.ndaysgame.NDaysAdBidBundle;
import adx.variants.ndaysgame.NDaysNCampaignsAgent;
import adx.variants.ndaysgame.NDaysNCampaignsGameServerOffline;
import adx.variants.ndaysgame.Tier1NDaysNCampaignsAgent;

public class MyNDaysNCampaignsAgent extends NDaysNCampaignsAgent {
	private static final String NAME = "Don Draper"; // TODO: enter a name. please remember to submit the Google form.
	private Map<String,Integer> freqMap;
	private Map<Campaign,Double> costMap = new HashMap<Campaign,Double> ();
	
	public MyNDaysNCampaignsAgent() {
		// TODO: fill this in (if necessary)
		freqMap = new HashMap<> ();
		List<Integer> userFreqs = Arrays.asList(4956,5044,4589,5411,8012,1988,2353,2603,3631,1325,2236,2808,
				4381,663,3816,773,4196,1215,1836,517,1795,808,1980,
				256,2401,407);
		int index = 0;
		for (MarketSegment m: MarketSegment.values()) {
			System.out.println(m.name());
			freqMap.put(m.name(),userFreqs.get(index));
			index = index + 1;
		}
	}
	
	@Override
	protected void onNewGame() {
		// TODO: fill this in (if necessary)
		costMap = new HashMap<Campaign,Double> ();
	}
	
	@Override
	protected Set<NDaysAdBidBundle> getAdBids() throws AdXException {
		// TODO: fill this in
		
		Set<NDaysAdBidBundle> bundles = new HashSet<>();
		
		for (Campaign c : this.getActiveCampaigns()) {
			HashSet<SimpleBidEntry> bidEntries = new HashSet<SimpleBidEntry>();
			double cost = c.getReach()*0.05;
			double bid = cost/c.getReach();
			double limit = (1.1*c.getReach() - super.getCumulativeReach(c))*0.05; // limit for campaign and segment
			limit = Math.min(limit,c.getBudget()- super.getCumulativeCost(c));
			MarketSegment segment = c.getMarketSegment();
			SimpleBidEntry bidEntry = new SimpleBidEntry(segment,bid,limit);
			bidEntries.add(bidEntry);
			NDaysAdBidBundle bundle = new NDaysAdBidBundle(c.getId(),limit, bidEntries);
			bundles.add(bundle);
		}
		
		return bundles;
	}

	@Override
	protected Map<Campaign, Double> getCampaignBids(Set<Campaign> campaignsForAuction) throws AdXException {
		// TODO: fill this in
		
		Map<Campaign, Double> bids = new HashMap<>();
		
		for (Campaign c : campaignsForAuction) {
			
			int reach = c.getReach();
		
			double est_cost = reach*0.05; // estimated cost
			int est_reach = (int) (0.8*reach); // estimated reach
			double eff_reach = super.effectiveReach(est_reach, reach); // effective reach
			
			double bid = est_cost/eff_reach;
			bids.put(c, super.clipCampaignBid(c, bid));
		}
		
		return bids;
	}

	public static void main(String[] args) throws IOException, AdXException {
		// Here's an opportunity to test offline against some TA agents. Just run
		// this file in Eclipse to do so.
		// Feel free to change the type of agents.
		// Note: this runs offline, so:
		// a) It's much faster than the online test; don't worry if there's no delays.
		// b) You should still run the test script mentioned in the handout to make sure
		// your agent works online.
		if (args.length == 0) {
			Map<String, AgentLogic> test_agents = new ImmutableMap.Builder<String, AgentLogic>()
					.put("me", new MyNDaysNCampaignsAgent())
					.put("opponent_1", new Tier1NDaysNCampaignsAgent())
					.put("opponent_2", new Tier1NDaysNCampaignsAgent())
					.put("opponent_3", new Tier1NDaysNCampaignsAgent())
					.put("opponent_4", new Tier1NDaysNCampaignsAgent())
					.put("opponent_5", new Tier1NDaysNCampaignsAgent())
					.put("opponent_6", new Tier1NDaysNCampaignsAgent())
					.put("opponent_7", new Tier1NDaysNCampaignsAgent())
					.put("opponent_8", new Tier1NDaysNCampaignsAgent())
					.put("opponent_9", new Tier1NDaysNCampaignsAgent()).build();

			// Don't change this.
			OfflineGameServer.initParams(new String[] { "offline_config.ini", "CS1951K-FINAL" });
			AgentStartupUtil.testOffline(test_agents, new NDaysNCampaignsGameServerOffline());
		} else {
			// Don't change this.
			AgentStartupUtil.startOnline(new MyNDaysNCampaignsAgent(), args, NAME);
		}
	}

}
