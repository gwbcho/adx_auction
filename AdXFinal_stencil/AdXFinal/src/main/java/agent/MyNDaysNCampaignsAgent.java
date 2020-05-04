package agent;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
	//private static final double RFACTOR = 1.0;
	private Map<MarketSegment,List<MarketSegment>> _baseMap;
	private List<MarketSegment> _baseSegs;
	private Map<MarketSegment,Integer> _freqMap;
	//private List<Campaign> _lostCamps;
	
	public MyNDaysNCampaignsAgent() throws AdXException {
		// TODO: fill this in (if necessary)
		
		//_lostCamps = new ArrayList<Campaign>();
		this.buildBaseSegments();
		this.buildFreqMap();
		Comparator<MarketSegment> freqComp = new Comparator<MarketSegment>() {
			public int compare(MarketSegment m1,MarketSegment m2) {
				if (_freqMap.get(m1) > _freqMap.get(m2)) {
					return -1;
				}
				return 1;
			}
		};
		_baseSegs.sort(freqComp);
		
	}
	
	private void buildBaseSegments() throws AdXException {
		_baseSegs = new ArrayList<MarketSegment>();
		int counter = 0;
		for (MarketSegment m: MarketSegment.values()) {
			if (counter > 17) {
				//System.out.println(m.name());
				_baseSegs.add(m);
			}
			counter += 1;
		}
		_baseMap = new HashMap<MarketSegment,List<MarketSegment>>();
		for (MarketSegment m: MarketSegment.values()) {
			List<MarketSegment> base = new ArrayList<>();
			for (MarketSegment bs: _baseSegs) {
				if (MarketSegment.marketSegmentSubset(m, bs)){
					//System.out.println(bs.name());
					base.add(bs);
				}
			}
			_baseMap.put(m,base);
		}
	}
	
	
	private void buildFreqMap(){
		_freqMap = new HashMap<MarketSegment,Integer> ();
		List<Integer> userFreqs = Arrays.asList(4956,5044,4589,5411,8012,1988,2353,2603,3631,1325,2236,2808,
				4381,663,3816,773,4196,1215,1836,517,1795,808,1980,
				256,2401,407);
		int index = 0;
		for (MarketSegment m: MarketSegment.values()) {
			//System.out.println(m.name().concat(Integer.toString(userFreqs.get(index))));
			_freqMap.put(m,userFreqs.get(index));
			index = index + 1;
		}
	}
	
	@Override
	protected void onNewGame() {
		// TODO: fill this in (if necessary)
		//_lostCamps = new ArrayList<Campaign>();
	}
	
	private double[] bidFunction(Campaign c,double reachFactor) {
		//double costLimit = c.getBudget() - super.getCumulativeCost(c);
		//double quality = super.getQualityScore();
		//double reachFactor = RFACTOR;
		double budget = c.getBudget();
		int totReach = c.getReach();
		int cumReach = super.getCumulativeReach(c);
		double currReach = super.effectiveReach(cumReach, totReach);
		double finReach = super.effectiveReach((int)reachFactor*totReach,totReach);
		double limit = budget*(finReach - currReach);
		//limit = 0*(reachFactor*totReach - cumReach) + 0.01;
		double denom = (reachFactor*totReach - cumReach);
		double bid = limit/denom;
		if (denom <= 0 || limit < 0) {
			bid = 0;
			limit = 1;
		}
		return new double[] {bid,limit};
	}
	
	public double effReach(Campaign camp) {
		int cumReach = super.getCumulativeReach(camp);
		return super.effectiveReach(cumReach,camp.getReach());
	}
	
	public Map<Campaign,Double> getEffList() {
		Map<Campaign,Double> map = new HashMap<Campaign,Double>();
		for (Campaign c : this.getActiveCampaigns()) {
			map.put(c, this.effReach(c));
		}
		return map;
	}
	
	
	public Set<NDaysAdBidBundle> check() throws AdXException {
		
		
		// Part 1
		
		
		Set<NDaysAdBidBundle> bundles = new HashSet<NDaysAdBidBundle>();
		
		
		Map<Campaign,List<MarketSegment>> cSegments = new HashMap<Campaign,List<MarketSegment>>();
		Map<Campaign,Integer> count = new HashMap<Campaign,Integer>();
		List<MarketSegment> others = new ArrayList<MarketSegment>();
		for (Campaign c : this.getActiveCampaigns()) {
			count.put(c,0);
			cSegments.put(c,new ArrayList<MarketSegment>());
		}
			
	
		for (MarketSegment bm: _baseSegs) {
			Map<Campaign,Double> effReach = this.getEffList();
			Comparator<Campaign> comp = new Comparator<Campaign>() {
				public int compare(Campaign c1,Campaign c2) {
					double score = 0;
					score = score - 100*(count.get(c1)-count.get(c2)) ;
					score = score - 10*(effReach.get(c1) - effReach.get(c2));
					score = score - 100*(c1.getEndDay() - c2.getEndDay());
					if (score > 0) {
						return -1;
					}
					return 1;
				}
			};
			List<Campaign> camps = new ArrayList<Campaign>();
			
			for (Campaign c : this.getActiveCampaigns()) {
				List<MarketSegment> bSegs = _baseMap.get(c.getMarketSegment());
				if (bSegs.contains(bm)) {
					camps.add(c);
				}
			}
			
			if (camps.size() > 0) {
				camps.sort(comp);
				Campaign selected = camps.get(0);
				count.put(selected,count.get(selected)+1);
				cSegments.containsKey(selected);
				cSegments.get(selected).add(bm); // is this added to map value as well?
			} else {
				others.add(bm);
			}
		}
		
		
		// Part 2
		
		
		int index = 0;
		for (Campaign camp: this.getActiveCampaigns()) {
			double reachFactor = 1.4;
			Set<SimpleBidEntry> bidEntries = new HashSet<SimpleBidEntry>();
			double seglimit = camp.getBudget() - super.getCumulativeCost(camp);
			double[] bidArr = this.bidFunction(camp,reachFactor);
			double cbid = bidArr[0];
			double limit = bidArr[1];
			limit = Math.min(limit,seglimit);
			List<MarketSegment> base_segs = cSegments.get(camp);
			//List<MarketSegment> base_segs = _baseMap.get(camp.getMarketSegment());
			for (MarketSegment bm: base_segs) {
				SimpleBidEntry bidEntry = new SimpleBidEntry(bm,cbid,seglimit);
				bidEntries.add(bidEntry);
			}
			if (index == 0) {
				index += 1;
				//for (MarketSegment bm:others) {
					//SimpleBidEntry bidEntry = new SimpleBidEntry(bm,0,1);
					//bidEntries.add(bidEntry);
				//}
			}
			NDaysAdBidBundle bundle = new NDaysAdBidBundle(camp.getId(),limit, bidEntries);
			bundles.add(bundle);
		}
		
		return bundles;	
		
	}
	
	
	
	/**
	public Set<SimpleBidEntry> userBidsHelper(Campaign camp,double campbid) throws AdXException {
		MarketSegment camp_seg = camp.getMarketSegment();
		double seglimit = camp.getBudget() - super.getCumulativeCost(camp);
		//seglimit = camp.getReach();
		Set<SimpleBidEntry> bidEntries = new HashSet<SimpleBidEntry>(); 
		List<MarketSegment> base_segs = new ArrayList<MarketSegment>();
		for (MarketSegment bm: _baseMap.get(camp_seg)) {base_segs.add(bm);}
		for (Campaign c: super.getActiveCampaigns()) {
			boolean bool1 = (c.getEndDay() < camp.getEndDay()) && (this.effReach(c) < 1.15);
			//boolean bool2 = this.effReach(c) < this.effReach(camp);
			if (bool1){
				for (MarketSegment bm: _baseMap.get(c.getMarketSegment())) {
					if (base_segs.contains(bm)) {
						base_segs.remove(bm);
					}
				}
			} 
		}
		for (MarketSegment bm: base_segs) {
			//double[] segArr = this.getSegmentBid(camp, bm);
			
			SimpleBidEntry bidEntry = new SimpleBidEntry(bm,campbid,seglimit);
			bidEntries.add(bidEntry);
		}
		
		return bidEntries;
	}
	*/
	
	@Override
	protected Set<NDaysAdBidBundle> getAdBids() throws AdXException {
		// TODO: fill this in
		
		//Set<NDaysAdBidBundle> bundles = new HashSet<>();
		/**
		for (Campaign c : this.getActiveCampaigns()) {
			if (_lostCamps.contains(c)){
				_lostCamps.remove(c);
			}
		}
		*/
		return this.check();
	}
	
	
	/**
	@Override
	protected Set<NDaysAdBidBundle> getAdBids() throws AdXException {
		// TODO: fill this in
		
		Set<NDaysAdBidBundle> bundles = new HashSet<>();
		/**
		for (Campaign c : this.getActiveCampaigns()) {
			if (_lostCamps.contains(c)){
				_lostCamps.remove(c);
			}
		}
		*/
		/**
		double reachFactor = 1.4;
		
		for (Campaign c : this.getActiveCampaigns()) {
			double[] bidArr = this.bidFunction(c,reachFactor);
			double cbid = bidArr[0];
			double limit = bidArr[1];
			Set<SimpleBidEntry> bidEntries = this.userBidsHelper(c,cbid);
			limit = Math.min(limit,c.getBudget() - super.getCumulativeCost(c));
			//limit = c.getBudget() - super.getCumulativeCost(c);
			NDaysAdBidBundle bundle = new NDaysAdBidBundle(c.getId(),limit, bidEntries);
			bundles.add(bundle);
		}
		
		return bundles;
	}
	*/
	
	
	
	
	@Override
	protected Map<Campaign, Double> getCampaignBids(Set<Campaign> campaignsForAuction) throws AdXException {
		// TODO: fill this in
		
		Map<Campaign, Double> bids = new HashMap<>();
		
		for (Campaign c : campaignsForAuction) {
			//_lostCamps.add(c);
			//double diff = c.getEndDay() - c.getStartDay();
			//double freq = _freqMap.get(c.getMarketSegment());
			//double reach = c.getReach();
			//double theta = reach/(diff*freq);
			
			double bid = c.getReach()*0.10;
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
		//new MyNDaysNCampaignsAgent();
		//new MyNDaysNCampaignsAgent();
		
		MyNDaysNCampaignsAgent agent = new MyNDaysNCampaignsAgent();
		//MyNDaysNCampaignsAgent agent3 = new MyNDaysNCampaignsAgent();
		if (args.length == 0) {
			Map<String, AgentLogic> test_agents = new ImmutableMap.Builder<String, AgentLogic>()
					.put("agent", agent)
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
		//System.out.println("Quality Score");
		
		
		
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
	
	/**
	public double campaignBidHelper(Campaign newC) throws AdXException {
		//List<MarketSegment> base_segs = _baseMap.get(newSegment);
		
		MarketSegment newSegment = newC.getMarketSegment();
		int count = 0;
		for (Campaign c: super.getActiveCampaigns()) {
			boolean subset = MarketSegment.marketSegmentSubset(c.getMarketSegment(),newSegment);
			if (subset) {
				count += 1;
			}	
		}
		// accept/reject campaign based on the count. 
		if (count > 1 ) {
			cbid = cbid/2;
		} 
			
	}
	*/

}
