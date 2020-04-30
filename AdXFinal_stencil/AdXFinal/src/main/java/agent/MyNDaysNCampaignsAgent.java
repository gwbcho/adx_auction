package agent;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
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
	private Map<MarketSegment,Integer> _freqMap;
	Map<MarketSegment,List<MarketSegment>> _baseMap;
	
	public MyNDaysNCampaignsAgent() throws AdXException {
		// TODO: fill this in (if necessary)
		_freqMap = new HashMap<MarketSegment,Integer> ();
		List<Integer> userFreqs = Arrays.asList(4956,5044,4589,5411,8012,1988,2353,2603,3631,1325,2236,2808,
				4381,663,3816,773,4196,1215,1836,517,1795,808,1980,
				256,2401,407);
		int index = 0;
		for (MarketSegment m: MarketSegment.values()) {
			System.out.println(m.name());
			_freqMap.put(m,userFreqs.get(index));
			index = index + 1;
		}
		List<MarketSegment> baseSegs = new ArrayList<MarketSegment>();
		int counter = 0;
		for (MarketSegment m: MarketSegment.values()) {
			if (counter > 16) {
				baseSegs.add(m);
			}
			counter += 1;
		}
		_baseMap = new HashMap<MarketSegment,List<MarketSegment>>();
		for (MarketSegment m: MarketSegment.values()) {
			List<MarketSegment> base = new ArrayList<>();
			for (MarketSegment bs: baseSegs) {
				if (MarketSegment.marketSegmentSubset(bs, m)){
					base.add(bs);
				}
			}
			_baseMap.put(m,base);
		}
	}
	
	
	@Override
	protected void onNewGame() {
		// TODO: fill this in (if necessary)
		
	}
	
	private double[] bidFunction(Campaign c,double reachFactor) {
		double budget = c.getBudget();
		int totReach = c.getReach();
		int cumReach = super.getCumulativeReach(c);
		//double cumCost = super.getCumulativeCost(c);
		//double quality = super.getQualityScore();
		double currReach = super.effectiveReach(cumReach, totReach);
		double finReach = super.effectiveReach((int)reachFactor*totReach,totReach);
		double limit = budget*(finReach - currReach);
		double bid = limit/(reachFactor*totReach - cumReach);
		bid = 0.05;
		return new double[] {bid,limit};
	}
	
	
	@Override
	protected Set<NDaysAdBidBundle> getAdBids() throws AdXException {
		// TODO: fill this in
		
		Set<NDaysAdBidBundle> bundles = new HashSet<>();
		
		for (Campaign c : this.getActiveCampaigns()) {
			double reachFactor = 1.1;
			double[] bidArr = this.bidFunction(c,reachFactor);
			double cbid = bidArr[0];
			double limit = bidArr[1];
			Set<SimpleBidEntry> bidEntries = this.userBidsHelper(c,cbid);
			limit = Math.min(0.1*limit,c.getBudget() - super.getCumulativeCost(c));
			//limit = c.getBudget() - super.getCumulativeCost(c);
			NDaysAdBidBundle bundle = new NDaysAdBidBundle(c.getId(),limit, bidEntries);
			bundles.add(bundle);
		}
		
		return bundles;
	}
	
	public Set<SimpleBidEntry> userBidsHelper(Campaign camp,double campbid) throws AdXException {
		double seglimit = camp.getBudget() - super.getCumulativeCost(camp);
		MarketSegment camp_seg = camp.getMarketSegment();
		Set<SimpleBidEntry> bidEntries = new HashSet<SimpleBidEntry>(); 
		List<MarketSegment> base_segs = new ArrayList<MarketSegment>();
		for (MarketSegment bm: _baseMap.get(camp_seg)) {base_segs.add(bm);}
		for (Campaign c: super.getActiveCampaigns()) {
			if (c.getEndDay() < camp.getEndDay()){
				for (MarketSegment bm: _baseMap.get(c.getMarketSegment())) {
					if (base_segs.contains(bm)) {
						//base_segs.remove(bm);
					}
				}
			}	
		}
		for (MarketSegment bm: base_segs) {
			SimpleBidEntry bidEntry = new SimpleBidEntry(bm,campbid,seglimit);
			bidEntries.add(bidEntry);
		}
		
		return bidEntries;
	}
	
	public double campaignBidHelper(Campaign newC) throws AdXException {
		MarketSegment newSegment = newC.getMarketSegment();
		//List<MarketSegment> base_segs = _baseMap.get(newSegment);
		int count = 0;
		for (Campaign c: super.getActiveCampaigns()) {
			boolean subset = MarketSegment.marketSegmentSubset(c.getMarketSegment(),newSegment);
			if (subset) {
				count += 1;
			}	
		}
		// accept/reject campaign based on the bids.
		double cbid = 0.10;
		if (count > 1 ) {
			cbid = cbid/2;
		} 
		
		return newC.getReach()*cbid;
	}
	
	
	@Override
	protected Map<Campaign, Double> getCampaignBids(Set<Campaign> campaignsForAuction) throws AdXException {
		// TODO: fill this in
		
		Map<Campaign, Double> bids = new HashMap<>();
		
		for (Campaign c : campaignsForAuction) {
			
			double bid = this.campaignBidHelper(c);
			bids.put(c, super.clipCampaignBid(c, 0));
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
	
	

	/**
	protected Set<NDaysAdBidBundle> getAdBidsOld() throws AdXException {
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
	*/

}
