import { SearchBudgetError } from "./search_budget.ts";

export const MAX_RESULTS = 5;

export type SearchCenter = {
  lat: number;
  lng: number;
};

export type PlaceSearchInput = {
  query: string;
  center: SearchCenter;
};

export type NaverSearch = (query: string) => Promise<unknown>;
export type NaverGeocode = (
  query: string,
  center: SearchCenter,
) => Promise<unknown>;
export type ShopSearch = () => Promise<RegisteredShop[]>;

export type RegisteredShop = {
  id: string;
  naverPlaceId: string | null;
  name: string;
  address: string;
  lat: number;
  lng: number;
};

export type PlaceSearchResult = {
  name: string;
  address: string;
  lat: number;
  lng: number;
  kind: "map_location" | "registered_shop";
  shop_id?: string;
};

type NaverLocalItem = {
  title?: unknown;
  link?: unknown;
  category?: unknown;
  address?: unknown;
  roadAddress?: unknown;
  mapx?: unknown;
  mapy?: unknown;
};

type NaverLocalResponse = {
  items?: unknown;
};

type NaverGeocodeAddress = {
  roadAddress?: unknown;
  jibunAddress?: unknown;
  x?: unknown;
  y?: unknown;
};

type NaverGeocodeResponse = {
  status?: unknown;
  addresses?: unknown;
};

type ParsedNaverItem = {
  name: string;
  address: string;
  lat: number;
  lng: number;
  category: string;
  naverPlaceId: string | null;
};

const STATION_DEDUPLICATION_DISTANCE_METERS = 300;
const SHOP_MATCH_DISTANCE_METERS = 100;
const RAIL_STATION_CATEGORY_MARKERS = [
  "지하철,전철",
  "지하철역",
  "전철역",
  "기차역",
  "철도역",
  "도시철도",
];
const ADMINISTRATIVE_REGION_SUFFIX_MIN_LENGTH = new Map([
  ["특별자치시", 6],
  ["특별자치도", 6],
  ["특별시", 4],
  ["광역시", 4],
  ["시", 3],
  ["도", 3],
  ["구", 3],
  ["군", 3],
  ["동", 3],
  ["읍", 3],
  ["면", 3],
  ["리", 3],
]);
const TOP_LEVEL_REGION_ALIASES = new Set([
  "서울",
  "부산",
  "대구",
  "인천",
  "광주",
  "대전",
  "울산",
  "세종",
  "경기",
  "강원",
  "충북",
  "충남",
  "전북",
  "전남",
  "경북",
  "경남",
  "제주",
]);

export function parseInput(value: unknown): PlaceSearchInput | null {
  if (!isRecord(value) || typeof value.query !== "string") return null;
  if (!isRecord(value.center)) return null;

  const query = value.query.trim();
  const lat = value.center.lat;
  const lng = value.center.lng;
  if (query.length === 0 || query.length > 100) return null;
  if (!isCoordinate(lat, -90, 90) || !isCoordinate(lng, -180, 180)) {
    return null;
  }

  return { query, center: { lat, lng } };
}

export async function searchPlaces(
  input: PlaceSearchInput,
  searchNaver: NaverSearch,
  geocodeNaver: NaverGeocode,
  searchShops: ShopSearch,
): Promise<PlaceSearchResult[]> {
  if (isStationQuery(input.query)) {
    return await searchStation(input.query, input.center, searchNaver);
  }
  if (isAdministrativeRegionQuery(input.query) || isAddressQuery(input.query)) {
    return await searchMapLocation(input, geocodeNaver);
  }

  const generalPayload = await searchNaver(input.query);
  const generalItems = parseNaverItems(generalPayload, input.center);
  const stationAliasResults = await searchStationAlias(
    input,
    searchNaver,
  );
  if (stationAliasResults.length > 0) return stationAliasResults;

  const shops = await searchShops();
  return matchRegisteredShops(generalItems, shops).slice(0, MAX_RESULTS);
}

export function matchRegisteredShops(
  items: ParsedNaverItem[],
  shops: RegisteredShop[],
): PlaceSearchResult[] {
  const results: PlaceSearchResult[] = [];
  const matchedShopIds = new Set<string>();
  for (const item of items) {
    const matchedShop = findRegisteredShop(item, shops);
    if (matchedShop === null || matchedShopIds.has(matchedShop.id)) continue;
    matchedShopIds.add(matchedShop.id);
    results.push({
      name: matchedShop.name,
      address: matchedShop.address,
      lat: matchedShop.lat,
      lng: matchedShop.lng,
      kind: "registered_shop",
      shop_id: matchedShop.id,
    });
  }
  return results;
}

export function isAdministrativeRegionQuery(query: string): boolean {
  const normalizedQuery = normalizeSearchText(query);
  if (normalizedQuery.length === 0 || normalizedQuery.endsWith("역")) {
    return false;
  }
  if (TOP_LEVEL_REGION_ALIASES.has(normalizedQuery)) return true;
  return [...ADMINISTRATIVE_REGION_SUFFIX_MIN_LENGTH].some(
    ([suffix, minimumLength]) =>
      normalizedQuery.length >= minimumLength && normalizedQuery.endsWith(suffix),
  );
}

export function isAddressQuery(query: string): boolean {
  const normalizedQuery = query.normalize("NFKC").trim();
  return /(?:로|길|번길)\s*\d/.test(normalizedQuery) ||
    /\d+(?:-\d+)?(?:번지)?$/.test(normalizedQuery);
}

export function stripTitleHtml(title: string): string {
  return title
    .replace(/<[^>]*>/g, "")
    .replaceAll("&amp;", "&")
    .replaceAll("&quot;", '"')
    .replaceAll("&#39;", "'")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .trim();
}

function parseNaverItems(
  value: unknown,
  center: SearchCenter,
): ParsedNaverItem[] {
  if (!isRecord(value)) return [];
  const items = (value as NaverLocalResponse).items;
  if (!Array.isArray(items)) return [];
  return items
    .map(parseNaverItem)
    .filter((item): item is ParsedNaverItem => item !== null)
    .sort((left, right) =>
      distanceSquared(left, center) - distanceSquared(right, center)
    )
    .slice(0, MAX_RESULTS);
}

function parseNaverItem(value: unknown): ParsedNaverItem | null {
  if (!isRecord(value)) return null;
  const item = value as NaverLocalItem;
  if (
    typeof item.title !== "string" ||
    typeof item.mapx !== "string" ||
    typeof item.mapy !== "string"
  ) {
    return null;
  }

  const lng = Number(item.mapx) / 10_000_000;
  const lat = Number(item.mapy) / 10_000_000;
  if (!isCoordinate(lat, -90, 90) || !isCoordinate(lng, -180, 180)) {
    return null;
  }
  const address = selectAddress(item.roadAddress, item.address);
  const name = stripTitleHtml(item.title);
  if (name.length === 0 || address.length === 0) return null;

  return {
    name,
    address,
    lat,
    lng,
    category: typeof item.category === "string" ? item.category : "",
    naverPlaceId: extractNaverPlaceId(item.link),
  };
}

export function extractNaverPlaceId(value: unknown): string | null {
  if (typeof value !== "string" || value.trim().length === 0) return null;
  try {
    const url = new URL(value);
    const match = url.pathname.match(
      /\/(?:place|restaurant|cafe|hospital|pharmacy)\/(\d{5,})(?:\/|$)/,
    );
    return match?.[1] ?? null;
  } catch {
    return null;
  }
}

function findRegisteredShop(
  item: ParsedNaverItem,
  shops: RegisteredShop[],
): RegisteredShop | null {
  if (item.naverPlaceId !== null) {
    const idMatch = shops.find((shop) =>
      shop.naverPlaceId === item.naverPlaceId
    );
    if (idMatch !== undefined) return idMatch;
  }

  const normalizedName = normalizeMatchText(item.name);
  return shops.find((shop) => {
    if (normalizeMatchText(shop.name) !== normalizedName) return false;
    const hasSameAddress = normalizeAddress(shop.address) ===
      normalizeAddress(item.address);
    const isNearby = distanceMeters(shop, item) <= SHOP_MATCH_DISTANCE_METERS;
    return hasSameAddress || isNearby;
  }) ?? null;
}

async function searchStation(
  query: string,
  center: SearchCenter,
  searchNaver: NaverSearch,
): Promise<PlaceSearchResult[]> {
  const payload = await searchNaver(query);
  const normalizedQuery = normalizeSearchText(query);
  const stationItems = parseNaverItems(payload, center)
    .filter((item) =>
      isRailStationCategory(item.category) &&
      normalizeSearchText(item.name).includes(normalizedQuery)
    )
    .sort((left, right) => compareStationRepresentatives(left, right, center));

  return stationItems
    .filter((item, index, allItems) =>
      allItems.slice(0, index).every((previous) =>
        distanceMeters(item, previous) > STATION_DEDUPLICATION_DISTANCE_METERS
      )
    )
    .map(toMapLocation)
    .slice(0, MAX_RESULTS);
}

async function searchStationAlias(
  input: PlaceSearchInput,
  searchNaver: NaverSearch,
): Promise<PlaceSearchResult[]> {
  const normalizedQuery = normalizeSearchText(input.query);
  if (normalizedQuery.length === 0 || normalizedQuery.includes(" ")) return [];
  return await searchStation(`${input.query}역`, input.center, searchNaver);
}

async function searchMapLocation(
  input: PlaceSearchInput,
  geocodeNaver: NaverGeocode,
): Promise<PlaceSearchResult[]> {
  try {
    const payload = await geocodeNaver(input.query, input.center);
    return parseNaverGeocodeResponse(payload, input.center, input.query);
  } catch (error) {
    if (error instanceof SearchBudgetError) throw error;
    return [];
  }
}

function parseNaverGeocodeResponse(
  value: unknown,
  center: SearchCenter,
  query: string,
): PlaceSearchResult[] {
  if (!isRecord(value)) return [];
  const response = value as NaverGeocodeResponse;
  if (response.status !== "OK" || !Array.isArray(response.addresses)) return [];

  return response.addresses
    .map((address) => parseNaverGeocodeAddress(address, query))
    .filter((result): result is PlaceSearchResult => result !== null)
    .sort((left, right) =>
      distanceSquared(left, center) - distanceSquared(right, center)
    )
    .slice(0, 1);
}

function parseNaverGeocodeAddress(
  value: unknown,
  query: string,
): PlaceSearchResult | null {
  if (!isRecord(value)) return null;
  const geocodeAddress = value as NaverGeocodeAddress;
  if (
    typeof geocodeAddress.x !== "string" ||
    typeof geocodeAddress.y !== "string"
  ) {
    return null;
  }
  const lng = Number(geocodeAddress.x);
  const lat = Number(geocodeAddress.y);
  if (!isCoordinate(lat, -90, 90) || !isCoordinate(lng, -180, 180)) {
    return null;
  }
  const address = selectAddress(
    geocodeAddress.roadAddress,
    geocodeAddress.jibunAddress,
  );
  if (address.length === 0) return null;
  return {
    name: query.trim(),
    address,
    lat,
    lng,
    kind: "map_location",
  };
}

function selectAddress(primary: unknown, fallback: unknown): string {
  if (typeof primary === "string" && primary.trim().length > 0) {
    return primary.trim();
  }
  return typeof fallback === "string" ? fallback.trim() : "";
}

function toMapLocation(item: ParsedNaverItem): PlaceSearchResult {
  return {
    name: item.name,
    address: item.address,
    lat: item.lat,
    lng: item.lng,
    kind: "map_location",
  };
}

function compareStationRepresentatives(
  left: ParsedNaverItem,
  right: ParsedNaverItem,
  center: SearchCenter,
): number {
  const distanceDifference = distanceSquared(left, center) -
    distanceSquared(right, center);
  if (distanceDifference !== 0) return distanceDifference;
  return normalizeSearchText(left.name).localeCompare(normalizeSearchText(right.name));
}

function isStationQuery(query: string): boolean {
  return normalizeSearchText(query).endsWith("역");
}

function isRailStationCategory(category: string): boolean {
  const normalizedCategory = normalizeSearchText(category);
  return RAIL_STATION_CATEGORY_MARKERS.some((marker) =>
    normalizedCategory.includes(normalizeSearchText(marker))
  );
}

function normalizeSearchText(value: string): string {
  return value.normalize("NFKC").toLocaleLowerCase().replace(/\s+/g, "");
}

function normalizeMatchText(value: string): string {
  return value.normalize("NFKC").toLocaleLowerCase().replace(/[^0-9a-z가-힣]/g, "");
}

function normalizeAddress(value: string): string {
  return normalizeMatchText(value)
    .replace(/^서울특별시/, "서울")
    .replace(/^부산광역시/, "부산")
    .replace(/^대구광역시/, "대구")
    .replace(/^인천광역시/, "인천")
    .replace(/^광주광역시/, "광주")
    .replace(/^대전광역시/, "대전")
    .replace(/^울산광역시/, "울산")
    .replace(/^세종특별자치시/, "세종")
    .replace(/^제주특별자치도/, "제주");
}

function distanceMeters(
  left: { lat: number; lng: number },
  right: { lat: number; lng: number },
): number {
  const earthRadiusMeters = 6_371_000;
  const latitudeDelta = degreesToRadians(right.lat - left.lat);
  const longitudeDelta = degreesToRadians(right.lng - left.lng);
  const leftLatitude = degreesToRadians(left.lat);
  const rightLatitude = degreesToRadians(right.lat);
  const haversine = Math.sin(latitudeDelta / 2) ** 2 +
    Math.cos(leftLatitude) * Math.cos(rightLatitude) *
      Math.sin(longitudeDelta / 2) ** 2;
  return 2 * earthRadiusMeters * Math.asin(Math.min(1, Math.sqrt(haversine)));
}

function degreesToRadians(value: number): number {
  return value * Math.PI / 180;
}

function distanceSquared(
  place: { lat: number; lng: number },
  center: SearchCenter,
): number {
  const lat = place.lat - center.lat;
  const lng = place.lng - center.lng;
  return lat * lat + lng * lng;
}

function isCoordinate(
  value: unknown,
  minimum: number,
  maximum: number,
): value is number {
  return typeof value === "number" && Number.isFinite(value) &&
    value >= minimum && value <= maximum;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
