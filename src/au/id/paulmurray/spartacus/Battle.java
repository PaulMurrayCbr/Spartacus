package au.id.paulmurray.spartacus;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Battle {
	public static final int MAXDICE = 6;
	public static final int MAXDICE1 = MAXDICE + 1;

	static boolean squelch_output = false;

	static void pp(int depth, Object s) {
		if (squelch_output)
			return;
		while (depth-- > 0) {
			System.out.print(" ");
		}
		System.out.println(s);
	}

	public Battle(String[] av) {
	}

	public static void main(String[] av) throws Throwable {
		new Battle(av).go();
	}

	private void go() throws FileNotFoundException {
		// caclulate_base_probs(); // only need to do this once :)
		// this will recursively entangle all the possible states

		initAttackerNeeds3();
		squelch_output = true;
		State.state(StateKey.sk(AD.ad(MAXDICE, MAXDICE), AD.ad(MAXDICE, MAXDICE))).calculate(0);
		squelch_output = false;
		findLocalMaxima();
		dumpHtml1("undefended-2.html");

		initAttackerNeeds2();
		squelch_output = true;
		State.clear();
		State.state(StateKey.sk(AD.ad(MAXDICE, MAXDICE), AD.ad(MAXDICE, MAXDICE))).calculate(0);
		squelch_output = false;
		findLocalMaxima();
		dumpHtml1("undefended-1.html");

		initAttackerAlwaysHits();
		squelch_output = true;
		State.clear();
		State.state(StateKey.sk(AD.ad(MAXDICE, MAXDICE), AD.ad(MAXDICE, MAXDICE))).calculate(0);
		squelch_output = false;
		findLocalMaxima();
		dumpHtml1("undefended-0.html");
	}

	private void dumpHtml1(String file) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new FileOutputStream(file));

		final int PRINTDICE = MAXDICE;

		pw.println("<HTML><BODY><TABLE border='1' cellspacing='0' cellpadding='1'>");
		pw.println("<TR><td></td><td></td>");
		for (int i = 1; i <= PRINTDICE; i++) {
			pw.println("<TD colspan='" + PRINTDICE + "'>da" + i + "</TD>");

		}
		pw.println("</TR>");
		pw.println("<TR><td></td><td></td>");
		for (int j = 1; j <= PRINTDICE; j++) {
			for (int i = 1; i <= PRINTDICE; i++) {
				pw.println("<TD>dd" + i + "</TD>");
			}
		}
		pw.println("</TR>");

		for (int aa = 1; aa <= PRINTDICE; aa++) {
			for (int ad = 1; ad <= PRINTDICE; ad++) {
				pw.print("<TR>");
				if (ad == 1) {
					pw.print("<td rowspan='" + (PRINTDICE) + "'>aa" + aa + "</td>");
				}
				pw.print("<td>ad" + ad + "</td>");
				for (int da = 1; da <= PRINTDICE; da++) {
					for (int dd = 1; dd <= PRINTDICE; dd++) {
						AD a = AD.ad(aa, ad);
						AD d = AD.ad(da, dd);
						StateKey sk = StateKey.sk(a, d);
						pw.print("<TD>");
						if (a.a < 1 || d.a + d.d <= 2) {
							pw.print("-");
						} else {
							if (d.a > State.state(sk).onHits[1].sk.invert().getDefender().a) {
								pw.print("<span style='color: red;'>A</span>");
							}
							if (d.d > State.state(sk).onHits[1].sk.invert().getDefender().d) {
								pw.print("<span style='color: black;'>D</span>");
							}

							final double w = State.state(sk).winProb;
							double r = Math.sqrt(1 - w);
							double g = Math.sqrt(w);

							String rh = Integer.toHexString((int) (255 * r));
							if (rh.length() < 2)
								rh = "0" + rh;
							String gh = Integer.toHexString((int) (255 * g));
							if (gh.length() < 2)
								gh = "0" + gh;

							pw.print("<span style='text-align: right; background-color: #" + rh + gh + "00;'>" + pct(w)
									+ "</span>");
						}
						pw.print("</TD>");
					}
				}
				pw.println("</TR>");
			}
		}
		pw.println("</TABLE></BODY></HTML>");
		pw.close();

	}

	private void findLocalMaxima() {
		boolean foundone = false;
		// and now it is time to theorem test!

		for (int aa = 1; aa <= MAXDICE; aa++) {
			for (int ad = 1; ad <= MAXDICE; ad++) {
				AD a = AD.ad(aa, ad);
				for (int da = 1; da <= MAXDICE; da++) {
					for (int dd = 1; dd <= MAXDICE; dd++) {
						AD d = AD.ad(da, dd);
						if (d.a + d.d < 4)
							continue;
						if (a.a < 2)
							continue;
						StateKey sk = StateKey.sk(a, d);
						State s = State.state(sk);
						if (s.onHits[1] == null) {
							throw new IllegalStateException(s.sk + " onhits[1] is null??");
						}

						State oneHitSituation = State.state(s.onHits[1].sk.invert());
						for (int hits = 2; hits <= a.a; hits++) {
							if (d.a + d.d - hits < 2)
								continue;

							if (!s.onHits[hits].sk.invert().equals(oneHitSituation.onHits[hits - 1].sk.invert())) {
								System.out.println("\n FOUND LOCAL MINIMUM");
								System.out.println("\nState: " + s.sk);
								System.out.println("one-hit recommended situation: " + oneHitSituation.sk);

								System.out.println("on " + hits + " hits");
								System.out.println("recommended situation (a): " + s.onHits[hits].sk.invert());
								System.out.println("recommended situation (b): "
										+ oneHitSituation.onHits[hits - 1].sk.invert());

								foundone = true;

							}
						}
					}
				}
			}
		}
		if (!foundone) {
			System.out.println("No local maxima found! Yay!");
		}

	}

	static final class StateKey {
		final int state;

		public String toString() {
			return "[A:" + ((state >> 24) & 255) + "/" + ((state >> 16) & 255) //
					+ " vs. D:" + ((state >> 8) & 255) + "/" + ((state >> 0) & 255) //
					+ " (" + getTotalDice() + ")" //
					+ "]";
		}

		static StateKey[] skv = new StateKey[MAXDICE1 * MAXDICE1 * MAXDICE1 * MAXDICE1];

		static {
			for (int aa = 0; aa <= MAXDICE; aa++)
				for (int ad = 0; ad <= MAXDICE; ad++) {
					AD a = AD.ad(aa, ad);
					for (int da = 0; da <= MAXDICE; da++)
						for (int dd = 0; dd <= MAXDICE; dd++) {
							AD d = AD.ad(da, dd);
							skv[aa * MAXDICE1 * MAXDICE1 * MAXDICE1 //
									+ ad * MAXDICE1 * MAXDICE1 //
									+ da * MAXDICE1 //
									+ dd] = new StateKey(a, d);
						}
				}
		}

		static StateKey sk(AD a, AD d) {
			return skv[a.a * MAXDICE1 * MAXDICE1 * MAXDICE1 //
					+ a.d * MAXDICE1 * MAXDICE1 //
					+ d.a * MAXDICE1 //
					+ d.d];
		}

		// an important sanity check
		int getTotalDice() {
			return ((state >> 24) & 255) + ((state >> 16) & 255) + ((state >> 8) & 255) + ((state >> 0) & 255);

		}

		AD getAttacker() {
			return AD.ad((state >> 24) & 255, (state >> 16) & 255);
		}

		AD getDefender() {
			return AD.ad((state >> 8) & 255, (state >> 0) & 255);
		}

		StateKey(AD attacker, AD defender) {
			state = attacker.a << 24 | attacker.d << 16 | defender.a << 8 | defender.d;
		}

		StateKey invert() {
			return sk(getDefender(), getAttacker());
		}

		public int hashCode() {
			return state;
		}

		public boolean equals(Object o) {
			return (o == this) || (o instanceof StateKey && hashCode() == o.hashCode());
		}
	}

	static final class State {
		final StateKey sk;

		// probability of a win
		double winProb;
		boolean iscomputed = false; // we have yet to calculate what to do

		// what you should do if you get 0,1,2 hits etc.
		// null means you lose
		State[] onHits = new State[MAXDICE1];

		static Map<StateKey, State> states = new HashMap<StateKey, State>();

		static {
			for (int aa = 0; aa <= MAXDICE; aa++)
				for (int ad = 0; ad <= MAXDICE; ad++) {
					AD a = AD.ad(aa, ad);
					for (int da = 0; da <= MAXDICE; da++)
						for (int dd = 0; dd <= MAXDICE; dd++) {
							AD d = AD.ad(da, dd);
							StateKey k = StateKey.sk(a, d);
							states.put(k, new State(k));
						}
				}

		}

		State(StateKey sk) {
			super();
			this.sk = sk;
		}

		static State state(StateKey k) {
			return states.get(k);
		}

		AD getSituation() {
			return AD.ad(sk.getAttacker().a, sk.getDefender().d);
		}

		static void clear() {
			for (State s : states.values())
				s.iscomputed = false;
		}

		void calculate(int depth) {
			if (iscomputed)
				return;

			// our inverse is computed iff this thing is computed. We do them as
			// pairs.

			State inv = State.state(sk.invert());

			pp(depth, "Calculate state for " + sk + " and " + inv.sk);
			depth++;
			workOutWhatToDo(depth);
			if (!sk.equals(inv.sk))
				inv.workOutWhatToDo(depth);
			depth--;

			// now, having worked out what to do given all the possible hits, we
			// need to calculate the winProb of
			// this state and its mirror. They are worked out in pairs because
			// of the markov chain that we get
			// when (potentially) the states bat back and forth scoring zero
			// hits

			double[] probs = baseProbs.get(AD.ad(sk.getAttacker().a, sk.getDefender().d));
			double[] invProbs = baseProbs.get(AD.ad(sk.getDefender().a, sk.getAttacker().d));

			// ok. the numbers I need are:

			double this2InvP = probs[0];
			double inv2ThisP = invProbs[0];

			double simpleWinP = 0;
			for (int i = 1; i <= MAXDICE; i++) {
				if (probs[i] != 0 && onHits[i] != null) {
					simpleWinP += probs[i] * (1 - onHits[i].winProb);
				}
			}

			double simpleInvWinP = 0;
			for (int i = 1; i <= MAXDICE; i++) {
				if (invProbs[i] != 0 && inv.onHits[i] != null) {
					simpleInvWinP += invProbs[i] * (1 - inv.onHits[i].winProb);
				}
			}

			// Right! After much blogging at
			// https://paulmurray.wordpress.com/2015/07/06/spartacus-3/
			// I belive this to be the correct pair of equations

			double loopP = this2InvP * inv2ThisP;
			double winP = (simpleWinP + this2InvP * (1 - simpleInvWinP) - loopP) / (1 - loopP);
			double invWinP = (simpleInvWinP + (inv2ThisP * (1 - simpleWinP) - loopP)) / (1 - loopP);

			this.winProb = winP;
			inv.winProb = invWinP;

			pp(depth, sk + " D: winprob " + pct(winP));
			if (!sk.equals(inv.sk))
				pp(depth, inv.sk + " D: winprob " + pct(invWinP));

			// finally, mark this state as computed

			iscomputed = true;
			inv.iscomputed = true;
		}

		void workOutWhatToDo(int depth) {
			// just to keep things happy, I'll calculate this even for
			// an impossible number of hits
			for (int i = 0; i <= MAXDICE; i++) {
				workOutWhatToDo(i, depth);
			}
		}

		void workOutWhatToDo(int nHits, int depth) {
			// for zero hits, we move to our mirror state.
			if (nHits == 0) {
				onHits[nHits] = State.state(sk.invert());
			}
			// if the amount of hits leaves us losing, then onHits gets put to
			// null
			else {
				// ok. iterate through the possible ways of allocating attack
				// and defence dice.
				// for each, get the win probability for the inverse situation.
				// find the *LOWEST*, and select that one as the what to do.

				AD defender = sk.getDefender();
				AD attacker = sk.getAttacker();

				for (int removeFromAttack = 0; removeFromAttack <= nHits; removeFromAttack++) {
					int removeFromDefence = nHits - removeFromAttack;

					if (removeFromAttack >= defender.a)
						continue;
					if (removeFromDefence >= defender.d)
						continue;

					AD newDefender = AD.ad(defender.a - removeFromAttack, defender.d - removeFromDefence);

					State invertedNewState = State.state(StateKey.sk(newDefender, attacker));

					invertedNewState.calculate(depth + 1); // recursive call.

					if (onHits[nHits] == null) {
						// first cab off the rank;
						onHits[nHits] = invertedNewState;
					} else {
						if (invertedNewState.winProb < onHits[nHits].winProb) {
							onHits[nHits] = invertedNewState;
						}
					}
				}

				if (onHits[nHits] == null) {
					// System.out.println(sk + ", " + nHits +
					// " hits: YOU LOSE! ");
				} else {
					int offAttack = (defender.a - onHits[nHits].sk.getAttacker().a);
					int offDefense = (defender.d - onHits[nHits].sk.getAttacker().d);

					pp(depth, sk + ", " + nHits + " hits. Take "//
							+ (offAttack != 0 ? offAttack + " off attack, " : "")//
							+ (offDefense != 0 ? offDefense + " off defence, " : "")//
							+ "go to state " + onHits[nHits].sk.invert() + " invert for attack winprob: "//
							+ pct(1 - onHits[nHits].winProb));
				}
			}

		}
	}

	// simple key for attack.defense
	static final class AD {
		final int a, d;
		static AD[] adv = new AD[MAXDICE1 * MAXDICE1];

		static {
			for (int a = 0; a <= MAXDICE; a++)
				for (int d = 0; d <= MAXDICE; d++)
					adv[a * MAXDICE1 + d] = new AD(a, d);
		}

		static AD ad(int a, int d) {
			return adv[a * MAXDICE1 + d];
		}

		AD(int a, int d) {
			super();
			if (a < 0 || a > MAXDICE || d < 0 || d > MAXDICE) {
				throw new IllegalArgumentException("AD(" + a + "," + d + ")");
			}
			this.a = a;
			this.d = d;
		}

		public int hashCode() {
			return (a << 4) | d; // meh
		}

		public boolean equals(Object o) {
			return (o == this) || (o instanceof AD && hashCode() == o.hashCode());
		}

		public String toString() {
			return a + "/" + d;
		}
	}

	// given a attack and d defence dice, what are the probablilities of getting
	// 0, 1, 2, ... MAXDICE hits?
	static Map<AD, double[]> baseProbs = new HashMap<Battle.AD, double[]>();

	static {
		initAttackerNeeds3();
	}

	static void initAttackerNeeds3() {
		baseProbs.put(AD.ad(0, 0), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 1), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 2), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 3), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 4), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 5), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 6), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 0), new double[] { 0.3333333333333333, 0.6666666666666666, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 1), new double[] { 0.5833333333333334, 0.4166666666666667, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 2), new double[] { 0.7453703703703703, 0.25462962962962965, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 3), new double[] { 0.8263888888888888, 0.1736111111111111, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 4), new double[] { 0.8740997942386831, 0.12590020576131689, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 5), new double[] { 0.9051568930041153, 0.09484310699588477, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 6), new double[] { 0.9267153920896205, 0.07328460791037951, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 0), new double[] { 0.1111111111111111, 0.4444444444444444, 0.4444444444444444, 0.0, 0.0,
				0.0, 0.0 });
		baseProbs.put(AD.ad(2, 1), new double[] { 0.2824074074074074, 0.41203703703703703, 0.3055555555555556, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 2), new double[] { 0.44830246913580246, 0.32407407407407407, 0.22762345679012347, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 3), new double[] { 0.6193415637860082, 0.25475823045267487, 0.12590020576131689, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 4), new double[] { 0.7227151920438958, 0.2013460219478738, 0.07593878600823045, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 5), new double[] { 0.7901306012802927, 0.16094750228623686, 0.04892189643347051, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 6), new double[] { 0.8369657112101814, 0.13000590611187318, 0.03302838267794544, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 0), new double[] { 0.037037037037037035, 0.2222222222222222, 0.4444444444444444,
				0.2962962962962963, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 1), new double[] { 0.12422839506172839, 0.27391975308641975, 0.38271604938271603,
				0.2191358024691358, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 2), new double[] { 0.24704218106995884, 0.2840792181069959, 0.3153292181069959,
				0.15354938271604937, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 3), new double[] { 0.3830375514403292, 0.2646604938271605, 0.21469907407407407,
				0.1376028806584362, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 4), new double[] { 0.5442493998628258, 0.2341070816186557, 0.148358910608139,
				0.07328460791037951, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 5), new double[] { 0.653474365569273, 0.20056370027434842, 0.1046578503657979,
				0.0413040837905807, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 6), new double[] { 0.7296491182111466, 0.17013263745999085, 0.075512795781893,
				0.024705448546969468, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 0), new double[] { 0.012345679012345678, 0.09876543209876543, 0.2962962962962963,
				0.3950617283950617, 0.19753086419753085, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 1), new double[] { 0.05156893004115226, 0.15213477366255143, 0.3065843621399177,
				0.3377057613168724, 0.15200617283950618, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 2), new double[] { 0.12626457475994513, 0.20066015089163236, 0.29314557613168724,
				0.2667181069958848, 0.11321159122085048, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 3), new double[] { 0.22248299611339734, 0.22567658321902148, 0.23902963534522176,
				0.23227809213534523, 0.08053269318701417, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 4), new double[] { 0.34515805993750953, 0.22723765432098766, 0.18899319844535895,
				0.15124885688157294, 0.08736223041457095, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 5), new double[] { 0.4943004829675354, 0.21467942672610882, 0.14761052526291724,
				0.09746860790402885, 0.04594095713940964, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 6), new double[] { 0.6035729132267269, 0.1933471698292943, 0.11420002812812241,
				0.06403715690570543, 0.024842731910150892, 0.0, 0.0 });
		baseProbs.put(AD.ad(5, 0), new double[] { 0.00411522633744856, 0.0411522633744856, 0.1646090534979424,
				0.3292181069958848, 0.3292181069958848, 0.13168724279835392, 0.0 });
		baseProbs.put(AD.ad(5, 1), new double[] { 0.020597565157750342, 0.0761102537722908, 0.19890260631001372,
				0.31935871056241427, 0.2812071330589849, 0.10382373113854595, 0.0 });
		baseProbs.put(AD.ad(5, 2), new double[] { 0.06058884887974394, 0.12304598193872886, 0.22207218792866942,
				0.28560099451303156, 0.22862368541380887, 0.08006830132601737, 0.0 });
		baseProbs.put(AD.ad(5, 3), new double[] { 0.12335617188690748, 0.16598139098460601, 0.21490626428898033,
				0.25933963477366256, 0.17643913846974546, 0.05997739959609816, 0.0 });
		baseProbs.put(AD.ad(5, 4), new double[] { 0.20542800656022964, 0.192208516708581, 0.19429083790580703,
				0.1951203925976731, 0.1697570555809582, 0.043195190646751, 0.0 });
		baseProbs.put(AD.ad(5, 5), new double[] { 0.3205863555849803, 0.2019061036702569, 0.16853414047549492,
				0.14208257522354317, 0.10991748510770716, 0.05697333993801758, 0.0 });
		baseProbs.put(AD.ad(5, 6), new double[] { 0.45852861330826233, 0.19876674522959745, 0.1417041680735138,
				0.10321154590626005, 0.06781548966042326, 0.029973437821943075, 0.0 });
		baseProbs.put(AD.ad(6, 0), new double[] { 0.0013717421124828531, 0.01646090534979424, 0.0823045267489712,
				0.2194787379972565, 0.3292181069958848, 0.26337448559670784, 0.0877914951989026 });
		baseProbs.put(AD.ad(6, 1), new double[] { 0.008005401234567902, 0.03554741083676269, 0.11374028349336991,
				0.2381973022405121, 0.30907064471879286, 0.22511574074074073, 0.07032321673525377 });
		baseProbs.put(AD.ad(6, 2), new double[] { 0.027643818587105625, 0.06837277091906721, 0.14708183299039781,
				0.2404120941929584, 0.2749616579027587, 0.1860949169333943, 0.05543290847431794 });
		baseProbs.put(AD.ad(6, 3), new double[] { 0.06510873120205253, 0.10889840296829752, 0.16702180736549307,
				0.23644739829421327, 0.23115700255296448, 0.14853424830437434, 0.042832409312604784 });
		baseProbs.put(AD.ad(6, 4), new double[] { 0.11878993306935766, 0.14525320734686448, 0.17287587361238124,
				0.2019302163245779, 0.2150577539416417, 0.11384566472336534, 0.032247350981811716 });
		baseProbs.put(AD.ad(6, 5), new double[] { 0.1935842638149743, 0.1702485424799037, 0.1667245337294027,
				0.1641619578081692, 0.15827741446722213, 0.12353641866377217, 0.023466869036555798 });
		baseProbs.put(AD.ad(6, 6), new double[] { 0.3033605979242933, 0.18373431894662343, 0.1530037774066171,
				0.13121211766393165, 0.10973765316331563, 0.08119746153618182, 0.037754073359037035 });

	}

	static void initAttackerNeeds2() {
		// attacker must roll 2+ on undefended
		baseProbs.put(AD.ad(0, 0), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 1), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 2), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 3), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 4), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 5), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 6), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 0), new double[] { 0.16666666666666666, 0.8333333333333334, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 1), new double[] { 0.5833333333333334, 0.4166666666666667, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 2), new double[] { 0.7453703703703703, 0.25462962962962965, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 3), new double[] { 0.8263888888888888, 0.1736111111111111, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 4), new double[] { 0.8740997942386831, 0.12590020576131689, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 5), new double[] { 0.9051568930041153, 0.09484310699588477, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 6), new double[] { 0.9267153920896205, 0.07328460791037951, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 0), new double[] { 0.027777777777777776, 0.2777777777777778, 0.6944444444444444, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 1), new double[] { 0.16666666666666666, 0.39351851851851855, 0.4398148148148148, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 2), new double[] { 0.44830246913580246, 0.32407407407407407, 0.22762345679012347, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 3), new double[] { 0.6193415637860082, 0.25475823045267487, 0.12590020576131689, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 4), new double[] { 0.7227151920438958, 0.2013460219478738, 0.07593878600823045, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 5), new double[] { 0.7901306012802927, 0.16094750228623686, 0.04892189643347051, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 6), new double[] { 0.8369657112101814, 0.13000590611187318, 0.03302838267794544, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 0), new double[] { 0.004629629629629629, 0.06944444444444445, 0.3472222222222222,
				0.5787037037037037, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 1), new double[] { 0.03935185185185185, 0.16203703703703703, 0.39351851851851855,
				0.4050925925925926, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 2), new double[] { 0.16666666666666666, 0.2667181069958848, 0.30877057613168724,
				0.2578446502057613, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 3), new double[] { 0.3830375514403292, 0.2646604938271605, 0.21469907407407407,
				0.1376028806584362, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 4), new double[] { 0.5442493998628258, 0.2341070816186557, 0.148358910608139,
				0.07328460791037951, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 5), new double[] { 0.653474365569273, 0.20056370027434842, 0.1046578503657979,
				0.0413040837905807, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 6), new double[] { 0.7296491182111466, 0.17013263745999085, 0.075512795781893,
				0.024705448546969468, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 0), new double[] { 7.716049382716049E-4, 0.015432098765432098, 0.11574074074074074,
				0.38580246913580246, 0.48225308641975306, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 1), new double[] { 0.008487654320987654, 0.05015432098765432, 0.18904320987654322,
				0.3959619341563786, 0.35635288065843623, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 2), new double[] { 0.04942558299039781, 0.1285150891632373, 0.2438485939643347,
				0.32891803840877915, 0.24929269547325103, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 3), new double[] { 0.16666666666666666, 0.2104802526291724, 0.23098493941472337,
				0.23249957133058985, 0.15936856995884774, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 4), new double[] { 0.34515805993750953, 0.22723765432098766, 0.18899319844535895,
				0.15124885688157294, 0.08736223041457095, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 5), new double[] { 0.4943004829675354, 0.21467942672610882, 0.14761052526291724,
				0.09746860790402885, 0.04594095713940964, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 6), new double[] { 0.6035729132267269, 0.1933471698292943, 0.11420002812812241,
				0.06403715690570543, 0.024842731910150892, 0.0, 0.0 });
		baseProbs.put(AD.ad(5, 0), new double[] { 1.286008230452675E-4, 0.0032150205761316874, 0.03215020576131687,
				0.16075102880658437, 0.4018775720164609, 0.4018775720164609, 0.0 });
		baseProbs.put(AD.ad(5, 1), new double[] { 0.001736111111111111, 0.013395919067215363, 0.06858710562414266,
				0.21744255829903977, 0.391803840877915, 0.30703446502057613, 0.0 });
		baseProbs.put(AD.ad(5, 2), new double[] { 0.012899377000457247, 0.047028606538637406, 0.1279756801554641,
				0.251396747828075, 0.3344157235939643, 0.2262838648834019, 0.0 });
		baseProbs.put(AD.ad(5, 3), new double[] { 0.05810316167504954, 0.1130079732510288, 0.17998161484529798,
				0.23428271700198142, 0.2564455208809633, 0.15817901234567902, 0.0 });
		baseProbs.put(AD.ad(5, 4), new double[] { 0.16666666666666666, 0.17911177316720012, 0.1881326842960931,
				0.1899336911929076, 0.17460826363486256, 0.10154692104226998, 0.0 });
		baseProbs.put(AD.ad(5, 5), new double[] { 0.3205863555849803, 0.2019061036702569, 0.16853414047549492,
				0.14208257522354317, 0.10991748510770716, 0.05697333993801758, 0.0 });
		baseProbs.put(AD.ad(5, 6), new double[] { 0.45852861330826233, 0.19876674522959745, 0.1417041680735138,
				0.10321154590626005, 0.06781548966042326, 0.029973437821943075, 0.0 });
		baseProbs.put(AD.ad(6, 0), new double[] { 2.143347050754458E-5, 6.430041152263374E-4, 0.008037551440329218,
				0.05358367626886145, 0.20093878600823045, 0.4018775720164609, 0.33489797668038407 });
		baseProbs.put(AD.ad(6, 1), new double[] { 3.429355281207133E-4, 0.003268604252400549, 0.021165552126200273,
				0.08996699245541838, 0.24332347393689988, 0.3803190729309556, 0.2616133687700046 });
		baseProbs.put(AD.ad(6, 2), new double[] { 0.0031024948559670784, 0.014622389879591526, 0.05232743674744703,
				0.13822206980643195, 0.2635721498247218, 0.3289799573235787, 0.19917350156226185 });
		baseProbs.put(AD.ad(6, 3), new double[] { 0.017573064319463497, 0.04699883782959915, 0.10216025567748818,
				0.17485435162957882, 0.2477649653254077, 0.26420920019814054, 0.14643932502032211 });
		baseProbs.put(AD.ad(6, 4), new double[] { 0.06553253838972718, 0.10387275027942895, 0.14708626522041016,
				0.17995634451895884, 0.20551653539327508, 0.19573875483708444, 0.10229681136111535 });
		baseProbs.put(AD.ad(6, 5), new double[] { 0.16666666666666666, 0.15921085644090782, 0.16167263220570346,
				0.16051796462207235, 0.154842918570982, 0.13113252771268352, 0.06595643378098416 });
		baseProbs.put(AD.ad(6, 6), new double[] { 0.3033605979242933, 0.18373431894662343, 0.1530037774066171,
				0.13121211766393165, 0.10973765316331563, 0.08119746153618182, 0.037754073359037035 });
	}

	static void initAttackerAlwaysHits() {
		baseProbs.put(AD.ad(0, 0), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 1), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 2), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 3), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 4), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 5), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(0, 6), new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 0), new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 1), new double[] { 0.5833333333333334, 0.4166666666666667, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 2), new double[] { 0.7453703703703703, 0.25462962962962965, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 3), new double[] { 0.8263888888888888, 0.1736111111111111, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 4), new double[] { 0.8740997942386831, 0.12590020576131689, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 5), new double[] { 0.9051568930041153, 0.09484310699588477, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(1, 6), new double[] { 0.9267153920896205, 0.07328460791037951, 0.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 0), new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 1), new double[] { 0.0, 0.4212962962962963, 0.5787037037037037, 0.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 2), new double[] { 0.44830246913580246, 0.32407407407407407, 0.22762345679012347, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 3), new double[] { 0.6193415637860082, 0.25475823045267487, 0.12590020576131689, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 4), new double[] { 0.7227151920438958, 0.2013460219478738, 0.07593878600823045, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 5), new double[] { 0.7901306012802927, 0.16094750228623686, 0.04892189643347051, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(2, 6), new double[] { 0.8369657112101814, 0.13000590611187318, 0.03302838267794544, 0.0,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 0), new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 1), new double[] { 0.0, 0.0, 0.3402777777777778, 0.6597222222222222, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 2), new double[] { 0.0, 0.2925668724279835, 0.3357767489711934, 0.37165637860082307,
				0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 3), new double[] { 0.3830375514403292, 0.2646604938271605, 0.21469907407407407,
				0.1376028806584362, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 4), new double[] { 0.5442493998628258, 0.2341070816186557, 0.148358910608139,
				0.07328460791037951, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 5), new double[] { 0.653474365569273, 0.20056370027434842, 0.1046578503657979,
				0.0413040837905807, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(3, 6), new double[] { 0.7296491182111466, 0.17013263745999085, 0.075512795781893,
				0.024705448546969468, 0.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 0), new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 1), new double[] { 0.0, 0.0, 0.0, 0.2925668724279835, 0.7074331275720165, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 2), new double[] { 0.0, 0.0, 0.20929783950617284, 0.3315757887517147,
				0.45912637174211246, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 3), new double[] { 0.0, 0.23995127457704618, 0.24603123571101967, 0.26291366598079563,
				0.25110382373113854, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 4), new double[] { 0.34515805993750953, 0.22723765432098766, 0.18899319844535895,
				0.15124885688157294, 0.08736223041457095, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 5), new double[] { 0.4943004829675354, 0.21467942672610882, 0.14761052526291724,
				0.09746860790402885, 0.04594095713940964, 0.0, 0.0 });
		baseProbs.put(AD.ad(4, 6), new double[] { 0.6035729132267269, 0.1933471698292943, 0.11420002812812241,
				0.06403715690570543, 0.024842731910150892, 0.0, 0.0 });
		baseProbs.put(AD.ad(5, 0), new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0 });
		baseProbs.put(AD.ad(5, 1), new double[] { 0.0, 0.0, 0.0, 0.0, 0.26150977366255146, 0.7384902263374485, 0.0 });
		baseProbs.put(AD.ad(5, 2), new double[] { 0.0, 0.0, 0.0, 0.1600758744855967, 0.32373471079103794,
				0.5161894147233653, 0.0 });
		baseProbs.put(AD.ad(5, 3), new double[] { 0.0, 0.0, 0.16047775205761317, 0.22138988911751256,
				0.28892318244170095, 0.3292091763831733, 0.0 });
		baseProbs.put(AD.ad(5, 4), new double[] { 0.0, 0.2126076238060763, 0.2015042922509272, 0.20404902072854747,
				0.20749028349336993, 0.1743487797210791, 0.0 });
		baseProbs.put(AD.ad(5, 5), new double[] { 0.3205863555849803, 0.2019061036702569, 0.16853414047549492,
				0.14208257522354317, 0.10991748510770716, 0.05697333993801758, 0.0 });
		baseProbs.put(AD.ad(5, 6), new double[] { 0.45852861330826233, 0.19876674522959745, 0.1417041680735138,
				0.10321154590626005, 0.06781548966042326, 0.029973437821943075, 0.0 });
		baseProbs.put(AD.ad(6, 0), new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 });
		baseProbs.put(AD.ad(6, 1), new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.23995127457704618, 0.7600487254229538 });
		baseProbs.put(AD.ad(6, 2), new double[] { 0.0, 0.0, 0.0, 0.0, 0.1283513612635269, 0.3158102804450541,
				0.555838358291419 });
		baseProbs.put(AD.ad(6, 3), new double[] { 0.0, 0.0, 0.0, 0.11411784995300513, 0.19829085933927754,
				0.30382837505715593, 0.3837629156505614 });
		baseProbs.put(AD.ad(6, 4), new double[] { 0.0, 0.0, 0.1363162274392877, 0.17252266787964232,
				0.20546907084053073, 0.24544737209774933, 0.24024466174278988 });
		baseProbs.put(AD.ad(6, 5), new double[] { 0.0, 0.19664010448860975, 0.17475597982801713, 0.17137692815236075,
				0.16936260640439155, 0.1649346074076191, 0.12292977371900174 });
		baseProbs.put(AD.ad(6, 6), new double[] { 0.3033605979242933, 0.18373431894662343, 0.1530037774066171,
				0.13121211766393165, 0.10973765316331563, 0.08119746153618182, 0.037754073359037035 });

	}

	private void caclulate_base_probs() {
		// screw math: I am going to do this the easy way

		int[] adice = new int[MAXDICE];
		int[] ddice = new int[MAXDICE];

		long[] hits = new long[MAXDICE1];

		for (int a = 0; a <= MAXDICE; a++) {
			for (int d = 0; d <= MAXDICE; d++) {
				double[] probs = new double[MAXDICE1];
				baseProbs.put(AD.ad(a, d), probs);

				Arrays.fill(probs, 0);
				Arrays.fill(hits, 0);

				for (int aperm = 0; aperm < nperms(a); aperm++) {
					for (int dperm = 0; dperm < nperms(d); dperm++) {
						getperm(a, aperm, adice);
						getperm(d, dperm, ddice);

						int hitct = 0;
						for (int i = 0; i < a; i++) {
							if (adice[i] > (i < d ? ddice[i] : 0)) {
								hitct++;
							}
						}

						hits[hitct]++;
					}

				}

				double fac = (double) nperms(a) * (double) nperms(d);

				for (int i = 0; i <= MAXDICE; i++) {
					probs[i] = hits[i] / fac;
				}

				System.out.print("baseProbs.put(AD.ad(" + a + "," + d + "), new double[]{");
				for (int i = 0; i <= MAXDICE; i++) {
					if (i > 0)
						System.out.print(", ");
					System.out.print(probs[i]);
				}
				System.out.println("});");

			}
		}
	}

	private static void getperm(int n, int idx, int[] out) {
		Arrays.fill(out, 0);
		for (int i = 0; i < n; i++) {
			out[i] = (idx % 6) + 1;
			idx /= 6;
		}

		for (int i = 0; i < n; i++)
			out[i] *= -1;
		Arrays.sort(out, 0, n);
		for (int i = 0; i < n; i++)
			out[i] *= -1;
	}

	private static int nperms(int n) {
		int p = 1;
		while (n-- > 0)
			p *= 6;
		return p;
	}

	@SuppressWarnings("unused")
	private static String showArray(int[] dice) {
		StringBuilder bb = new StringBuilder();
		bb.append('[');
		for (int i = 0; i < dice.length; i++) {
			if (i != 0)
				bb.append(',');
			bb.append(dice[i]);
		}
		bb.append(']');
		return bb.toString();
	}

	private static Double pct(double x) {
		return (int) (x * 1000) / 10.0;
	}

	@SuppressWarnings("unused")
	private static String showArray(long[] dice) {
		StringBuilder bb = new StringBuilder();
		bb.append('[');
		for (int i = 0; i < dice.length; i++) {
			if (i != 0)
				bb.append(',');
			bb.append(dice[i]);
		}
		bb.append(']');
		return bb.toString();
	}

}
