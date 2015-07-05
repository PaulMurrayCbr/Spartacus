package au.id.paulmurray.spartacus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Battle {
	public static final int MAXDICE = 6;
	public static final int MAXDICE1 = MAXDICE + 1;

	public Battle(String[] av) {
	}

	public static void main(String[] av) throws Throwable {
		new Battle(av).go();
	}

	private void go() {
		caclulate_base_probs();

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

		public AD(int a, int d) {
			super();
			this.a = a;
			this.d = d;
		}

		public int hashCode() {
			return (a << 4) | d; // meh
		}

		public boolean equals(Object o) {
			return (o == this) || (o instanceof AD && hashCode() == o.hashCode());
		}
	}

	// given a attack and d defence dice, what are the probablilities of getting
	// 0, 1, 2, ... MAXDICE hits?
	Map<AD, double[]> baseProbs = new HashMap<Battle.AD, double[]>();

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
							if (adice[i] > (i < d ? ddice[i] : 2)) {
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

	private void getperm(int n, int idx, int[] out) {
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

	private int nperms(int n) {
		int p = 1;
		while (n-- > 0)
			p *= 6;
		return p;
	}

}
